package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;

/**
 * 统一认证的接口
 */
public interface AuthService
{
    /**
     * 认证方法
     * @param authParamsDto 认证参数
     * @return  用户信息
     */
    XcUserExt execute(AuthParamsDto authParamsDto);
}
