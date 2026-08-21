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

    //update-begin---author:cursor---date:2026-08-21---for:【后台新增用户】统一写 salt+密码哈希，避免 App 登录 NPE---
    /**
     * 按明文密码生成 8 位 per-user 盐并写入加密后的 password/salt（与 App 注册算法一致）
     */
    void applyPassword(WxUser user, String plainPassword);

    /**
     * 管理端新增/导入：默认密码 123456，补齐 loginType=phone 与占位 openid
     */
    void prepareAdminCreatedUser(WxUser user);
    //update-end---author:cursor---date:2026-08-21---for:【后台新增用户】统一写 salt+密码哈希，避免 App 登录 NPE---
}
