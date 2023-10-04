package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignClient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

//测试远程调用媒资服务
@SpringBootTest
public class FeignUploadTest
{
    @Resource
    MediaServiceClient mediaServiceClient;

    @Test
    public void test() throws IOException
    {
        //将file类型转化为MultipartFile类型
        File file = new File("E:\\test\\18.html");
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);

        String upload = mediaServiceClient.upload(multipartFile, "course/18.html");

        if(upload == null) System.err.println("走了降级逻辑");
    }
}
