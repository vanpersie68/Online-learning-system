package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class UserServiceImpl implements UserDetailsService
{
    @Resource
    ApplicationContext applicationContext;

    @Resource
    XcMenuMapper xcMenuMapper;

    //传入的请求认证参数是AuthParamsDto
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException
    {
        //将传入的json转成AuthParamsDto对象
        AuthParamsDto authParamsDto = null;
        try
        {
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException("请求认证的参数不符合要求");
        }

        //认证类型， 有password，微信。。。
        String authType = authParamsDto.getAuthType();

        String beanName = authType + "_authservice";
        //根据认证的类型，从spring容器中取出指定的Bean
        AuthService authService = applicationContext.getBean(beanName, AuthService.class);
        //调用统一execute方法完成认证
        XcUserExt xcUserExt = authService.execute(authParamsDto);

        //封装xcUserExt 用户信息为UserDetails
        return getUserPrincipal(xcUserExt);
    }

    /**
     * 查询用户信息
     * @param xcUserExt 用户id，主键
     * @return 用户信息
     */
    public UserDetails getUserPrincipal(XcUserExt xcUserExt)
    {
        String password = xcUserExt.getPassword();
        //根据用户id查询用户的权限
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(xcUserExt.getId());
        String[] authorize = null;

        if(xcMenus.size() > 0)
        {
            List<String> permission = new ArrayList<>();
            for (XcMenu xcMenu : xcMenus)
            {
                //拿到了用户拥有的权限标识符
                permission.add(xcMenu.getCode());
            }

            //将permission转成数组
            authorize = permission.toArray(new String[0]);
            xcUserExt.setPermissions(permission);
        }

        // 用户敏感信息不要设置
        xcUserExt.setPassword(null);

        //将用户信息转成json
        String userString = JSON.toJSONString(xcUserExt);
        return User.withUsername(userString).password(password).authorities(authorize).build();
    }
}
