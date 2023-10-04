package com.xuecheng.auth.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 重写了DaoAuthenticationProvider的校验的密码的方法。
 * 因为我们统一认证入口，导致一些认证方式不需要校验密码
 */
@Component
public class DaoAuthenticationProviderCustom extends DaoAuthenticationProvider
{
    // 由于DaoAuthenticationProvider调用UserDetailsService，所以这里需要注入一个
    @Resource
    public void setUserDetailsService(UserDetailsService userDetailsService)
    {
        super.setUserDetailsService(userDetailsService);
    }

    // 屏蔽密码对比，因为不是所有的认证方式都需要校验密码
    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException
    {
        // 里面啥也不写就不会校验密码了
    }
}
