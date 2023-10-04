package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.config.MinioConfig;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/9/10 8:58
 * @version 1.0
 */
@Service
@Slf4j
public class MediaFileServiceImpl implements MediaFileService
{
    @Resource
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MinioClient minioClient;

    @Resource
    MediaProcessMapper mediaProcessMapper;

    @Value("${minio.bucket.files}")
    private String bucket_MediaFiles; //存储普通文件的桶

    @Value("${minio.bucket.videofiles}")
    private String bucket_video; //存储视频的桶

    @Autowired
    MediaFileService currentProxy;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto)
    {
         //构建查询条件对象
         LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();
         //分页对象
         Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
         // 查询数据内容获得结果
         Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
         // 获取数据列表
         List<MediaFiles> list = pageResult.getRecords();
         // 获取数据总数
         long total = pageResult.getTotal();
         // 构建结果集
         PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
         return mediaListResult;
    }

    //根据扩展名来获取mimeType
    private String getMimeType(String extension)
    {
        if(extension == null)
        {
            extension = "";
        }

        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE; //通用mimeType，字节流
        if(extensionMatch != null)
        {
            mimeType = extensionMatch.getMimeType();
        }

        return mimeType;
    }

    //将文件上传到Minio
    public boolean addMediaFilesToMinio(String bucket, String localFilePath, String objectName, String mimeType)
    {
        try
        {
            UploadObjectArgs testBucket = UploadObjectArgs.builder()
                    .bucket(bucket) //桶
                    .filename(localFilePath) //指定本地文件的路径
                    .object(objectName) //对象名
                    .contentType(getMimeType(mimeType))
                    .build();

            minioClient.uploadObject(testBucket);
            log.debug("上传文件成功, bucket:{}, objectName:{}", bucket, objectName);

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            log.error("上传文件出错, bucket:{}, objectName:{}, 错误信息:{}", bucket, objectName, e.getMessage());
        }

        return false;
    }

    //获取文件默认存储目录路径 年/月/日
    private String getDefaultFolderPath()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String folder = sdf.format(new Date()).replace("-", "/")+"/";
        //2023/04/16/
        return folder;
    }

    //获取文件的md5
    private String getFileMd5(File file)
    {
        try (FileInputStream fileInputStream = new FileInputStream(file))
        {
            String fileMd5 = DigestUtils.md5Hex(fileInputStream);
            return fileMd5;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    @Transactional
    public MediaFiles addMediaFilesToDatabase(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName)
    {
        //2. 将文件信息保存到数据库中
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if(mediaFiles == null)
        {
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            //文件id
            mediaFiles.setId(fileMd5);
            //机构id
            mediaFiles.setCompanyId(companyId);
            //桶
            mediaFiles.setBucket(bucket);
            //file_path
            mediaFiles.setFilePath(objectName);
            //file_id
            mediaFiles.setFileId(fileMd5);
            //url
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            //create_date
            mediaFiles.setCreateDate(LocalDateTime.now());
            //status
            mediaFiles.setStatus("1");
            //audit_status
            mediaFiles.setAuditStatus("002003");
            //插入数据库

            int result = mediaFilesMapper.insert(mediaFiles);

            if(result <= 0)
            {
                log.error("向数据库保存文件失败, bucket:{}, objectName: {}", bucket, objectName);
                return null;
            }

            //添加到待处理任务表
            addWaitingTask(mediaFiles);

            log.debug("保存文件信息到数据库成功,{}", mediaFiles.toString());
            return mediaFiles;
        }

        return mediaFiles;
    }

    private void addWaitingTask(MediaFiles mediaFiles)
    {
        //文件名称
        String filename = mediaFiles.getFilename();
        //文件扩展名
        String exension = filename.substring(filename.lastIndexOf("."));
        //获取文件的MimeType
        String mimeType = getMimeType(exension);
        //通过mimeType判断如果是avi视频才写入待处理任务
        if(mimeType.equals("video/x-msvideo"))
        {
            //向mediaProcess表中插入记录
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles,mediaProcess);
            mediaProcess.setStatus("1");//未处理
            mediaProcess.setCreateDate(LocalDateTime.now());
            mediaProcess.setFailCount(0);//失败次数默认为0
            mediaFiles.setUrl(null);
            mediaProcessMapper.insert(mediaProcess);
        }
    }

    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath, String objectName)
    {
        //1. 将文件上传到Minio
        //获取文件默认在minio中存储的目录路径 如2023/01/02/12313123.jpg （12313123.jpg 是md5值）
        String defaultFolderPath = getDefaultFolderPath();
        //获取文件的Md5值
        String fileMd5 = getFileMd5(new File(localFilePath));

        //如果传入的objectName为空，说明传入的不是课程的静态页面
        if(StringUtils.isEmpty(objectName))
        {
            //按照默认的年月日去存储
            objectName = defaultFolderPath + fileMd5;
        }

        //文件名
        String filename = uploadFileParamsDto.getFilename();
        //获取扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        //获取mimeType
        String mimeType = getMimeType(extension);
        //上传文件到minio
        boolean result = addMediaFilesToMinio(bucket_MediaFiles, localFilePath, objectName, mimeType);
        if(!result)
        {
            XueChengPlusException.cast("上传文件失败");
        }

        //2. 将文件信息保存到数据库中
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDatabase(companyId, fileMd5, uploadFileParamsDto, bucket_MediaFiles, objectName);

        if(mediaFiles == null)
        {
            XueChengPlusException.cast("文件上传后保存文件信息失败");
        }

        //返回的对象
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);

        return uploadFileResultDto;
    }

    /**
     * @param fileMd5 文件的md5
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @description 检查文件是否存在
     */
    @Override
    public RestResponse<Boolean> checkFile(String fileMd5)
    {
        //先查询数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        //如果数据库存在，再查询 minio
        if(mediaFiles != null)
        {
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(mediaFiles.getBucket())
                    .object(mediaFiles.getFilePath())
                    .build();

            try
            {
                FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
                if(inputStream != null)
                {
                    //文件已经存在
                    return RestResponse.success(true);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        //文件不存在
        return RestResponse.success(false);
    }

    /**
     * @param fileMd5    文件的md5
     * @param chunkIndex 分块序号
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @description 检查分块是否存在
     */
    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex)
    {
        //分块文件所在目录的路径； md5值前两位 + 文件的md5值 + chunk
        String filePath = fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/chunk/";

        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucket_video)
                .object(filePath + chunkIndex)
                .build();

        try
        {
            FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
            if(inputStream != null)
            {
                //文件已经存在
                return RestResponse.success(true);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return RestResponse.success(false);
    }

    /**
     * @param fileMd5 文件md5
     * @param chunk   分块序号
     * @param localChunkFilePath   本地文件路径
     * @return com.xuecheng.base.model.RestResponse
     * @description 上传分块
     */
    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath)
    {
        //分块文件所在目录的路径； md5值前两位 + 文件的md5值 + chunk
        String filePath = fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + "chunk" + "/";
        //获取mimeType
        String mimeType = getMimeType(null);
        String objectName = filePath + chunk;

        //将分块文件上传到minio
        boolean result = addMediaFilesToMinio(bucket_video, localChunkFilePath, objectName, mimeType);
        if(!result)
        {
            return RestResponse.validfail(false, "上传分块文件失败");
        }

        return RestResponse.success(true);
    }

    /**
     * 得到合并后文件的路径
     * @param fileMd5 文件id的Md5值
     * @param fileExt 文件扩展名
     * @return 合并后文件的路径
     */
    private String getFilePathMd5(String fileMd5, String fileExt)
    {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

    /**
     * 从minio下载文件
     * @param bucket 桶
     * @param objectName 对象名称
     * @return 下载后的文件
     */
    public File downloadFileFromMinIO(String bucket,String objectName)
    {
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try
        {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile=File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream,outputStream);
            return minioFile;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if(outputStream!=null)
            {
                try
                {
                    outputStream.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 清除分块文件
     * @param chunkFileFolderPath 分块文件路径
     * @param chunkTotal 分块文件总数
     */
    private void clearChunkFiles(String chunkFileFolderPath, int chunkTotal)
    {
        Iterable<DeleteObject> objects = Stream.iterate(0, i -> ++i).limit(chunkTotal)
                .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i)))).collect(Collectors.toList());

        RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket(bucket_video).objects(objects).build();
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);

        //要想真正删除，需要遍历并且get一下
        results.forEach(f -> {
            try
            {
                DeleteError deleteError = f.get();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    /**
     * @param companyId           机构id
     * @param fileMd5             文件md5
     * @param chunkTotal          分块总和
     * @param uploadFileParamsDto 文件信息
     * @return com.xuecheng.base.model.RestResponse
     * @description 合并分块
     */
    @Override
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto)
    {
        //分块文件所在目录的路径； md5值前两位 + 文件的md5值 + chunk
        String filePath = fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + "chunk" + "/";
        //找到分块文件调用minio的sdk进行文件合并
        List<ComposeSource> sourceList = Stream.iterate(0, i -> ++i).limit(chunkTotal)
                .map(i -> ComposeSource.builder().bucket(bucket_video).object(filePath + i).build()).collect(Collectors.toList());

        //源文件的名称
        String filename = uploadFileParamsDto.getFilename();
        String fileExt = filename.substring(filename.lastIndexOf("."));
        //合并后文件的ObjectName
        String objectName = getFilePathMd5(fileMd5, fileExt);

        //指定合并后的objectName等信息
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket(bucket_video)
                .object(objectName) //最终合并后的文件的ObjectName
                .sources(sourceList) //指定源文件
                .build();

        //1. 合并文件
        try
        {
            minioClient.composeObject(composeObjectArgs);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            log.error("合并文件出错, bucket:{}, objectName:{}, 错误信息:{}", bucket_video, objectName, e.getMessage());
            return RestResponse.validfail(false, "合并文件出错");
        }

        //2. 校验合并后的文件和源文件是否一致，一致则视频上传成功 (想要校验，需要从Minio中下载视频，然后校验)
        //从Minio下载文件
        File minioFile = downloadFileFromMinIO(bucket_video, objectName);
        //计算合并后文件的Md5
        try(FileInputStream fileInputStream = new FileInputStream(minioFile))
        {
            String mergeFileMD5 = DigestUtils.md5Hex(fileInputStream);
            //比较原始的Md5 和 合并后文件的Md5
            if(!mergeFileMD5.equals(fileMd5))
            {
                log.error("合共文件与源文件Md5值不一致, 原始文件:{}, 合并文件:{}", fileMd5, mergeFileMD5);
                return RestResponse.validfail(false, "文件校验失败");
            }
            //文件的大小
            uploadFileParamsDto.setFileSize(minioFile.length());
        }
        catch (Exception e)
        {
            return RestResponse.validfail(false, "文件校验失败");
        }

        //3. 将文件信息入库
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDatabase(companyId, fileMd5, uploadFileParamsDto, bucket_video, objectName);
        if(mediaFiles == null)
        {
            return RestResponse.validfail(false, "文件入库失败");
        }

        //4. 清除minio中的分块文件
        clearChunkFiles(filePath, chunkTotal);
        return RestResponse.success(true);
    }

    //根据媒资id查询文件信息
    @Override
    public MediaFiles getFileById(String mediaId)
    {
        return mediaFilesMapper.selectById(mediaId);
    }
}
