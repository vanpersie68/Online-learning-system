package com.xuecheng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication
@EnableFeignClients(basePackages={"com.xuecheng.content.feignClient"})
public class ContentApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(ContentApplication.class,args);
    }
}
