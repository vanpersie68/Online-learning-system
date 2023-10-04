package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignClient.MediaServiceClient;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.cotent.model.dto.CourseBaseInfoDto;
import com.xuecheng.cotent.model.dto.CoursePreviewDto;
import com.xuecheng.cotent.model.dto.TeachplanDto;
import com.xuecheng.cotent.model.po.CourseBase;
import com.xuecheng.cotent.model.po.CourseMarket;
import com.xuecheng.cotent.model.po.CoursePublish;
import com.xuecheng.cotent.model.po.CoursePublishPre;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
public class CoursePublishServiceImpl implements CoursePublishService
{
    @Resource
    CourseBaseInfoService courseBaseInfoService;

    @Resource
    TeachplanService teachplanService;

    @Resource
    CourseMarketMapper courseMarketMapper;

    @Resource
    CoursePublishPreMapper coursePublishPreMapper;

    @Resource
    CourseBaseMapper courseBaseMapper;

    @Resource
    CoursePublishMapper coursePublishMapper;

    @Resource
    MqMessageService mqMessageService;

    @Resource
    MediaServiceClient mediaServiceClient;

    /**
     * @param courseId 课程id
     * @return com.xuecheng.content.model.dto.CoursePreviewDto
     * @description 获取课程预览信息
     */
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId)
    {
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();

        //课程基本信息、营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        //课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);

        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplanTree);

        return coursePreviewDto;
    }

    /**
     * @param companyId
     * @param courseId  课程id
     * @return void
     * @description 提交审核
     */
    @Override
    @Transactional
    public void commitAudit(Long companyId, Long courseId)
    {
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);

        if(courseBaseInfo == null) XueChengPlusException.cast("课程找不到");

        //课程的审核状态为已提交则不允许提交
        String auditStatus = courseBaseInfo.getAuditStatus();
        if(auditStatus.equals("202003"))
            XueChengPlusException.cast("当前为等待审核状态，审核完成可以再次提交。");

        //本机构只允许提交本机构的课程
        if(!courseBaseInfo.getCompanyId().equals(companyId))
            XueChengPlusException.cast("不允许提交其它机构的课程。");

        //课程的图片 不允许提交
        if(StringUtils.isEmpty(courseBaseInfo.getPic()))
            XueChengPlusException.cast("提交失败，请上传课程图片");

        //计划信息没有填写 不允许提交
        if(teachplanTree == null || teachplanTree.size() == 0)
            XueChengPlusException.cast("请编写课程计划");


        //查询到课程的基本信息，营销信息，计划信息到 课程预发布表
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfo, coursePublishPre);

        //营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //转json
        String courseMarketJson = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(courseMarketJson);

        //课程计划信息
        String teachplanJson = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(teachplanJson);

        //教学机构id
        coursePublishPre.setCompanyId(companyId);
        //状态为已提交
        coursePublishPre.setStatus("202003");
        //提交时间
        coursePublishPre.setCreateDate(LocalDateTime.now());

        CoursePublishPre coursePublishPreTemp = coursePublishPreMapper.selectById(courseId);
        //插入
        if(coursePublishPreTemp == null)
        {
            //表中不存在该课程信息, 添加信息 到预发布表中
            coursePublishPreMapper.insert(coursePublishPre);
        }
        else
        {
            //表中已经存在该课程信息了，改为更新
            coursePublishPreMapper.updateById(coursePublishPre);
        }

        //更新课程基本信息表的审核状态为已提交
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);
    }

    /**
     * @param companyId 机构id
     * @param courseId  课程id
     * @return void
     * @description 课程发布
     */
    @Override
    @Transactional
    public void publish(Long companyId, Long courseId)
    {
        //查询预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre == null) XueChengPlusException.cast("课程没有审核记录，无法发布");

        //课程如果没有审核通过，不允许发布
        String status = coursePublishPre.getStatus();
        if(!status.equals("202004"))
        {
            XueChengPlusException.cast("课程没有审核通过不允许发布");
        }

        //向课程发布表写入数据
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre, coursePublish);
        //先查询课程发布表，如果有则更新，没有则插入
        CoursePublish coursePublishTemp = coursePublishMapper.selectById(coursePublish);
        if(coursePublishTemp == null)
            coursePublishMapper.insert(coursePublish);
        else
            coursePublishMapper.updateById(coursePublish);

        //向消息表写入数据
        saveCoursePublishMessage(courseId);

        //删除课程预发布表数据
        coursePublishPreMapper.deleteById(courseId);
    }

    /**
     * @description 保存消息表记录
     * @param courseId  课程id
     * @return void
     */
    private void saveCoursePublishMessage(Long courseId)
    {
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if(mqMessage == null)
        {
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
    }

    /**
     * @param courseId 课程id
     * @return File 静态化文件
     * @description 课程静态化
     */
    @Override
    public File generateCourseHtml(Long courseId)
    {
        Configuration configuration = new Configuration(Configuration.getVersion());
        //最终的静态文件
        File htmlFile = null;
        try
        {
            //拿到classpath路径
            String classpath = this.getClass().getResource("/").getPath();
            //指定模板的目录
            configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
            //指定编码
            configuration.setDefaultEncoding("utf-8");
            //模板
            Template template = configuration.getTemplate("course_template.ftl");
            //数据
            CoursePreviewDto coursePreviewDto = this.getCoursePreviewInfo(courseId);
            HashMap<Object, Object> map = new HashMap<>();
            map.put("model", coursePreviewDto);
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);

            //输入流
            InputStream inputStream = IOUtils.toInputStream(html);
            htmlFile = File.createTempFile("coursepublish", ".html");
            //输出文件
            FileOutputStream fileOutputStream = new FileOutputStream(htmlFile);
            //使用流将html写入文件
            IOUtils.copy(inputStream, fileOutputStream);
        }
        catch (Exception e)
        {
            log.error("页面静态化出现问题，课程id{}", courseId, e);
            e.printStackTrace();
        }

        return htmlFile;
    }

    /**
     * @param courseId
     * @param file     静态化文件
     * @return void
     * @description 上传课程静态化页面到minio
     */
    @Override
    public void uploadCourseHtml(Long courseId, File file)
    {
        try
        {
            //将file类型转化为MultipartFile类型
            MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
            String objectName = courseId + ".html";
            String upload = mediaServiceClient.upload(multipartFile, "course/" + objectName);
            if(upload == null)
            {
                log.debug("远程调用走降级逻辑得到的上传结果为null，课程id：{}", courseId);
                XueChengPlusException.cast("上传静态文件过程中存在异常");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            XueChengPlusException.cast("上传静态文件过程中存在异常");
        }
    }
}
