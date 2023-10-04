package com.xuecheng.content.service.jobhandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignClient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.cotent.model.dto.CoursePreviewDto;
import com.xuecheng.cotent.model.po.CoursePublish;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.search.po.CourseIndex;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

/**
 * 课程发布的任务类
 */
@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract
{
    @Resource
    CoursePublishService coursePublishService;

    @Resource
    SearchServiceClient searchServiceClient;

    @Resource
    CoursePublishMapper coursePublishMapper;

    //任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception
    {
        //分片参数
        int shardIndex = XxlJobHelper.getShardIndex(); //执行器的序号，从0开始
        int shardTotal = XxlJobHelper.getShardTotal(); //执行器总数

        //调用抽象类来执行任务
        process(shardIndex, shardTotal, "course_publish", 30, 60);
    }

    //执行课程发布任务的逻辑，如果此方法抛出异常说明任务执行失败
    @Override
    public boolean execute(MqMessage mqMessage)
    {
        //从mqMessage中拿到 business_key1（courseId）
        Long courseId = Long.parseLong(mqMessage.getBusinessKey1());

        //课程静态化上传到Minio
        generateCourseHtml(mqMessage, courseId);

        //向ElasticSearch写索引数据
        saveCourseIndex(mqMessage, courseId);

        //向Redis写缓存
        //saveCourseCache(mqMessage, courseId);

        //返回true表示任务完成
        return true;
    }

    //生成课程静态化页面并上传至文件系统
    public void generateCourseHtml(MqMessage mqMessage,long courseId)
    {
        log.info("开始进行课程静态化,课程id:{}",courseId);

        //消息id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();

        //任务幂等性处理

        //查询数据库，取出该阶段的执行状态
        int stageOne = mqMessageService.getStageOne(taskId);
        if(stageOne == 1)
        {
            log.info("课程静态化任务完成，无需处理...");
            return;
        }

        //开始进行课程静态化, 生成html页面
        File file = coursePublishService.generateCourseHtml(courseId);

        if(file == null)
        {
            XueChengPlusException.cast("生成的静态页面为null");
        }
        //将html上传到minio
        coursePublishService.uploadCourseHtml(courseId, file);

        //任务处理完成，将该阶段任务状态设置为完成
        this.getMqMessageService().completedStageOne(taskId);
    }

    //保存课程索引信息
    public void saveCourseIndex(MqMessage mqMessage,long courseId)
    {
        log.info("保存课程索引信息,课程id:{}",courseId);

        //消息id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();

        //取出第二阶段任务状态
        int stageTwo = mqMessageService.getStageTwo(taskId);
        if(stageTwo > 0)
        {
            log.info("课程索引信息已经写入，无需执行...");
            return;
        }

        //远程调用搜索服务添加索引接口，然后添加课程索引信息
        Boolean result = saveCourseIndex(courseId);
        if(result)
        {
            //任务处理完成，将该阶段任务状态设置为完成
            mqMessageService.completedStageTwo(taskId);
        }
    }

    //远程调用搜索服务添加索引接口，然后添加课程索引信息
    private Boolean saveCourseIndex(Long courseId)
    {
        //查询课程信息，远程调用搜索服务添加索引接口
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish, courseIndex);
        //远程调用
        Boolean add = searchServiceClient.add(courseIndex);
        if(!add) XueChengPlusException.cast("添加索引失败");

        return add;
    }

    //将课程信息缓存至redis
    public void saveCourseCache(MqMessage mqMessage,long courseId)
    {
        log.info("将课程信息缓存至redis,课程id:{}",courseId);

        //消息id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();

        //取出第三阶段任务状态
        int stageThree = mqMessageService.getStageThree(taskId);
        if(stageThree > 0)
        {
            log.info("课程缓存信息已经写入，无需执行...");
            return;
        }

        //查询课程信息，调用缓存服务添加缓存...
        //todo

        //任务处理完成，将该阶段任务状态设置为完成
        mqMessageService.completedStageThree(taskId);
    }
}
