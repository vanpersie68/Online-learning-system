package com.xuecheng.content;

import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.cotent.model.dto.CoursePreviewDto;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;


@SpringBootTest
public class FreemarkerTest
{
    @Resource
    CoursePublishService coursePublishService;

    //测试页面静态化
    @Test
    public void testGenerateHtmlByTemplate() throws IOException, TemplateException
    {
        Configuration configuration = new Configuration(Configuration.getVersion());

        //拿到classpath路径
        String classpath = this.getClass().getResource("/").getPath();
        System.err.println(classpath);
        //指定模板的目录
        configuration.setDirectoryForTemplateLoading(new File("E:\\xuecheng-plus-project\\xuecheng-plus-content\\xuecheng-plus-content-service\\src\\test\\resources\\templates\\"));
        //指定编码
        configuration.setDefaultEncoding("utf-8");
        //模板
        Template template = configuration.getTemplate("course_template.ftl");
        //数据
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(18L);
        HashMap<Object, Object> map = new HashMap<>();
        map.put("model", coursePreviewInfo);
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);

        //输入流
        InputStream inputStream = IOUtils.toInputStream(html, "utf-8");
        //输出文件
        FileOutputStream fileOutputStream = new FileOutputStream("E:\\test\\18.html");
        //使用流将html写入文件
        IOUtils.copy(inputStream, fileOutputStream);
    }
}
