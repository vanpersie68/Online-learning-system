package com.xuecheng.content.feignClient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Component
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient>
{
    //使用FallbackFactory，可以拿到熔断时的异常信息
    @Override
    public MediaServiceClient create(Throwable throwable)
    {
        //发生熔断，上传服务会调用此方法执行降级逻辑
        return new MediaServiceClient()
        {
            @Override
            public String upload(MultipartFile filedata, String objectName) throws IOException
            {
                //降级方法
                log.debug("调用媒资管理服务上传文件时发生熔断，异常信息:{}", throwable.toString(), throwable);
                return null;
            }
        };
    }
}
