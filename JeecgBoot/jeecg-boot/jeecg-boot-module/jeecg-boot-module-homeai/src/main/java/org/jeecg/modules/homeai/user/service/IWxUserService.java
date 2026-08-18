package org.jeecg.modules.homeai.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.user.entity.WxUser;

import java.util.Map;

/**
 * 微信用户 Service
 */
public interface IWxUserService extends IService<WxUser> {

    /**
     * 微信登录
     * @param code 微信临时 code（wx.login 获取）
     * @return { token, refreshToken, userInfo }
     */
    Map<String, Object> login(String code);

    /**
     * 刷新 Token
     * @param refreshToken 刷新令牌
     * @return { token, refreshToken }
     */
    Map<String, Object> refreshToken(String refreshToken);

    /**
     * 根据 openid 查询用户
     * @param openid 微信 openid
     * @return WxUser
     */
    WxUser getByOpenid(String openid);

    //update-begin---author:admin ---date:2026-08-17 for:【Android迁移】手机号密码登录---
    /**
     * 根据手机号查询用户（未删除）
     * @param phone 手机号
     * @return WxUser，不存在返回 null
     */
    WxUser getByPhone(String phone);

    /**
     * 手机号注册用户
     * @param user 调用方填充 phone/password/salt/loginType/nickname 的用户对象
     * @return 注册后的用户（含主键 id）
     */
    WxUser registerByPhone(WxUser user);
    //update-end---author:admin ---date:2026-08-17 for:【Android迁移】手机号密码登录---
}
