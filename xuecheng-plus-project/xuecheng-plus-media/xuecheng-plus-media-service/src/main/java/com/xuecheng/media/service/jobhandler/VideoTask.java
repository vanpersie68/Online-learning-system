package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 视频处理任务
 */
@Slf4j
@Component
public class VideoTask
{
    @Resource
    MediaFileProcessService mediaFileProcessService;

    @Resource
    MediaFileService mediaFileService;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath;

    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception
    {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        //取出cpu核心数作为一次处理数据的条数
        int processors = Runtime.getRuntime().availableProcessors();

        //一次处理视频数量不要超过cpu核心数
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
        int size = mediaProcessList.size();

        log.debug("取出待处理视频任务{}条", size);
        if (size <= 0)
        {
            return;
        }

        //启动size个线程的线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(size);
        //计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        //将处理任务加入线程池
        mediaProcessList.forEach(mediaProcess ->
        {
            threadPool.execute(() ->
            {
                try
                {
                    //任务id
                    Long taskId = mediaProcess.getId();
                    //抢占任务
                    boolean b = mediaFileProcessService.startTask(taskId);
                    if (!b)
                    {
                        log.error("抢占任务失败，任务id：{}", taskId);
                        return;
                    }

                    //桶
                    String bucket = mediaProcess.getBucket();
                    //存储路径
                    String objectName = mediaProcess.getFilePath();
                    //原始视频的md5值
                    String fileId = mediaProcess.getFileId();

                    //从minio将要处理的文件下载到服务器上
                    File file = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                    if (file == null)
                    {
                        log.debug("下载待处理文件失败,任务ID: {}", taskId);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "下载待处理文件失败");
                        return;
                    }

                    //原avi视频的路径
                    String videoPath = file.getAbsolutePath();
                    //转化后MP4文件的名称
                    String mp4Name = fileId + ".mp4";

                    //先创建一个临时文件，作为转换后的文件
                    File mp4File = null;
                    try
                    {
                        mp4File = File.createTempFile("mp4", ".mp4");
                    }
                    catch (IOException e)
                    {
                        log.error("创建mp4临时文件失败,{}", e.getMessage());
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "创建mp4临时文件失败");
                        return;
                    }

                    //转换后MP4的路径
                    String mp4Path = mp4File.getAbsolutePath();
                    //开始处理视频
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, videoPath, mp4Name, mp4Path);

                    //开始视频转换，成功将返回success
                    String result = videoUtil.generateMp4();

                    if (!result.equals("success"))
                    {
                        log.error("视频转码失败,bucket:{},objectName:{}", bucket ,objectName);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "视频转码失败");
                        return;
                    }

                    //将mp4上传至minio
                    boolean b1 = mediaFileService.addMediaFilesToMinio(bucket, mp4File.getAbsolutePath(), objectName, "video/mp4");
                    if(!b1)
                    {
                        log.error("上传MP4到Minio失败, taskid: {}", taskId);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "上传MP4到Minio失败");
                        return;
                    }


                    String url = getFilePath(fileId , ".mp4");
                    //保存文件成功, 需要更新任务处理状态
                    mediaFileProcessService.saveProcessFinishStatus(taskId, "2", fileId, url, null);
                }
                finally
                {
                    countDownLatch.countDown();
                }
            });
        });
        //等待,给一个充裕的超时时间,防止无限等待，到达超时时间还没有处理完成则结束任务
        countDownLatch.await(30, TimeUnit.MINUTES);
    }

    private String getFilePath(String fileMd5, String fileExt)
    {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }
}
