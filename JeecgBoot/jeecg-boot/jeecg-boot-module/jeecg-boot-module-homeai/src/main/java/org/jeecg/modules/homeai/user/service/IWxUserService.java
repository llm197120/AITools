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
}
