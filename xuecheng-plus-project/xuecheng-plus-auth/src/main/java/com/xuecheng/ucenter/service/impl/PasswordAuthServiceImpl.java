package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 账号和密码方式认证
 */
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService
{
    @Resource
    XcUserMapper xcUserMapper;
    @Resource
    PasswordEncoder passwordEncoder;
    @Resource
    CheckCodeClient checkCodeClient;

    /**
     * 认证方法
     * @param authParamsDto 认证参数
     * @return 用户信息
     */
    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto)
    {
        //验证码对应的key
        String checkcodekey = authParamsDto.getCheckcodekey();
        //前端输入的验证码
        String checkcode = authParamsDto.getCheckcode();

        if(StringUtils.isEmpty(checkcode) || StringUtils.isEmpty(checkcodekey)) throw new RuntimeException("请输入验证码");

        //远程调用验证码服务接口去校验验证码
        Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
        if(verify == null || !verify) throw new RuntimeException("验证码输入错误");

        //账号是否存在
        String name = authParamsDto.getUsername();
        QueryWrapper<XcUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", name);
        //根据username查询数据库
        XcUser user = xcUserMapper.selectOne(queryWrapper);
        //返回NULL表示用户不存在，SpringSecurity会帮我们处理，框架抛出异常用户不存在
        if(user == null) throw new RuntimeException("账号不存在");

        //验证密码是否正确
        //数据库中的密码
        String passwordFromDB = user.getPassword();
        //输入的密码
        String passwordFromType = authParamsDto.getPassword();
        boolean matches = passwordEncoder.matches(passwordFromType, passwordFromDB);//加密的密码放在后面
        if(!matches) throw new RuntimeException("密码不正确");

        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user, xcUserExt);
        return xcUserExt;
    }
}
