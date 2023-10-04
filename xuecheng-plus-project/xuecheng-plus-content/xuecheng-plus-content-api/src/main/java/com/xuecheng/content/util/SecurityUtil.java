package com.xuecheng.content.util;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;
import java.time.LocalDateTime;

@Slf4j
public class SecurityUtil
{
    public static XcUser getUser()
    {
        try
        {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof String)
            {
                //去除用户身份信息
                String userJson = principal.toString();
                //将json转化为对象
                XcUser xcUser = JSON.parseObject(userJson, XcUser.class);
                return xcUser;
            }
        }
        catch (Exception e)
        {
            log.error("获取当前登录用户身份信息出错：{}", e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // 这里使用内部类，是为了不让content工程去依赖auth工程
    @Data
    public static class XcUser implements Serializable
    {
        private static final long serialVersionUID = 1L;
        private String id;
        private String username;
        private String password;
        private String salt;
        private String name;
        private String nickname;
        private String wxUnionid;
        private String companyId;
        /**
         * 头像
         */
        private String userpic;
        private String utype;
        private LocalDateTime birthday;
        private String sex;
        private String email;
        private String cellphone;
        private String qq;

        /**
         * 用户状态
         */
        private String status;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }
}