package com.xuecheng.auth.controller;

import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Controller
public class WxLoginController
{
    @Resource
    WxAuthService wxAuthService;

    @RequestMapping("/wxLogin")
    public String wxLogin(String code, String state) throws IOException
    {
        log.debug("微信扫码回调,code:{},state:{}",code,state);
        //远程调用微信，申请令牌, 到令牌查询用户信息，将用户信息写入本项目数据库
        XcUser xcUser = wxAuthService.wxAuth(code);

        if(xcUser == null)
        {
            //return "redirect:http://localhost/error.html";
            return "redirect:http://www.51xuecheng.cn/error.html";
        }
        String username = xcUser.getUsername();

        //重定向到浏览器自动登录
        return "redirect:http://www.51xuecheng.cn/sign.html?username="+username+"&authType=wx";
        //return "redirect:http://localhost/sign.html?username="+username+"&authType=wx";
    }
}
