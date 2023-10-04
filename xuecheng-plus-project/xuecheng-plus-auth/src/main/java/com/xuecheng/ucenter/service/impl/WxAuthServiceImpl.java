package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 微信扫码登录
 */
@Slf4j
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService, WxAuthService
{
    @Resource
    XcUserMapper xcUserMapper;

    @Resource
    XcUserRoleMapper xcUserRoleMapper;

    @Value("${weixin.appid}")
    String appid;

    @Value("${weixin.secret}")
    String secret;

    @Resource
    RestTemplate restTemplate;

    @Resource
    WxAuthServiceImpl currentProxy;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto)
    {
        String username = authParamsDto.getUsername();
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername,username));
        if(xcUser == null) throw new RuntimeException("账号不存在");

        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser, xcUserExt);
        return xcUserExt;
    }

    /**
     * 微信扫码认证，申请令牌，携带令牌查询用户信息，保存用户信息到数据库
     * @param code 授权码
     * @return
     */
    @Override
    public XcUser wxAuth(String code)
    {
        //申请令牌
        Map<String, String> access_token_map = getAccess_token(code);
        String access_token = access_token_map.get("access_token");
        String openid = access_token_map.get("openid");

        //携带令牌查询用户信息
        Map<String, String> userinfo = getUserinfo(access_token, openid);

        //保存用户信息到数据库
        XcUser user = currentProxy.addWxUser(userinfo);

        return user;
    }

    /**
     * 携带授权码申请令牌
     * https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
     * @param code 授权码
     * @return
     * 最后响应的格式如下
     * {
     *     "access_token":"ACCESS_TOKEN",
     *     "expires_in":7200,
     *     "refresh_token":"REFRESH_TOKEN",
     *     "openid":"OPENID",
     *     "scope":"SCOPE",
     *     "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
     * }
     */
    private Map<String,String> getAccess_token(String code)
    {
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        //最终请求路径
        String wxUrl = String.format(url, appid, secret, code);
        log.info("调用微信接口申请access_token, url:{}", wxUrl);

        //远程调用url
        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.POST, null, String.class);
        //获取响应结果
        String result = exchange.getBody();
        log.info("调用微信接口申请access_token: 返回值:{}", result);

        //将result转换成Map
        Map<String, String> resultMap = JSON.parseObject(result, Map.class);
        return resultMap;
    }

    /**
     * 通过令牌查询用户信息
     * https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID
     *
     *      * 最后响应的格式如下
     *      * {
     *      *     "openid":"OPENID",
     *      *     "nickname":"NICKNAME",
     *      *     "sex":1,
     *      *     "province":"PROVINCE",
     *      *     "city":"CITY",
     *      *     "country":"COUNTRY",
     *      *     "headimgurl": "https://thirdwx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0",
     *      *     "privilege":[
     *      *         "PRIVILEGE1",
     *      *         "PRIVILEGE2"
     *      *     ],
     *      *     "unionid": " o6_bmasdasdsad6_2sgVt7hMZOPfL"
     *      * }
     * @param access_token
     * @param openid
     * @return
     */
    private Map<String,String> getUserinfo(String access_token, String openid)
    {
        String url = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        String wxUrl = String.format(url, access_token, openid);

        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.GET, null, String.class);
        //String result = exchange.getBody();

        //解决乱码问题
        String result = new String(exchange.getBody().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);

        //将result转换成Map
        Map<String, String> resultMap = JSON.parseObject(result, Map.class);
        return resultMap;
    }

    /**
     * 向数据库保存用户信息，如果用户不存在将其保存在数据库。
     * @param user_info_map
     * @return
     */
    @Transactional
    public XcUser addWxUser(Map<String, String> user_info_map)
    {
        String unionid = user_info_map.get("unionid");
        String nickname = user_info_map.get("nickname");
        //根据unionid查询用户信息
        QueryWrapper<XcUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("wx_unionid", unionid);
        XcUser xcUser = xcUserMapper.selectOne(queryWrapper);
        if(xcUser != null) return xcUser;

        //向数据库新增记录
        xcUser = new XcUser();
        String userId = UUID.randomUUID().toString();
        xcUser.setId(userId); //主键
        xcUser.setUsername(unionid); //首次登录后 用unionid充当用户名
        xcUser.setPassword(unionid); //首次登录后 用unionid充当密码
        xcUser.setWxUnionid(unionid);
        xcUser.setUsername(nickname);
        xcUser.setUserpic(user_info_map.get("headimgurl"));
        xcUser.setName(nickname);
        xcUser.setUtype("101001");//学生类型
        xcUser.setStatus("1");//用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);

        //向用户角色关系表新增记录
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(userId);
        xcUserRole.setRoleId("17");//学生角色
        xcUserRoleMapper.insert(xcUserRole);

        return xcUser;
    }
}
