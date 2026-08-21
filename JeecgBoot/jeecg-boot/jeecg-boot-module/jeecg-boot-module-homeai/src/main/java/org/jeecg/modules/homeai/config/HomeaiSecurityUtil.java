package org.jeecg.modules.homeai.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.CommonAPI;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.TokenUtils;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.service.IWxUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * HomeAI 模块安全工具
 * <p>
 * 统一处理两类认证来源：
 * 1. 管理端：JeecgBoot 标准 JWT（X-Access-Token，username 声明）
 * 2. 小程序端：HomeAI 独立 JWT（X-Access-Token，openid 声明）
 * </p>
 */
@Slf4j
@Component
public class HomeaiSecurityUtil {

    @Autowired
    private IWxUserService wxUserService;

    @Lazy
    @Autowired
    private CommonAPI commonApi;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 从请求头或参数中提取 token
     */
    public String getToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String token = request.getHeader("X-Access-Token");
        if (oConvertUtils.isEmpty(token)) {
            token = request.getParameter("token");
        }
        return token;
    }

    /**
     * 判断当前请求是否来自管理端（JeecgBoot 标准 JWT 有效）
     */
    public boolean isConsoleAuthenticated(HttpServletRequest request) {
        String token = getToken(request);
        if (oConvertUtils.isEmpty(token)) {
            return false;
        }
        try {
            String username = JwtUtil.getUsername(token);
            if (oConvertUtils.isEmpty(username)) {
                return false;
            }
            return TokenUtils.verifyToken(token, commonApi, redisUtil);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取管理端登录用户（未登录返回 null）
     */
    public LoginUser getConsoleUser(HttpServletRequest request) {
        if (!isConsoleAuthenticated(request)) {
            return null;
        }
        try {
            String username = JwtUtil.getUsername(getToken(request));
            return TokenUtils.getLoginUser(username, commonApi, redisUtil);
        } catch (Exception e) {
            log.warn("获取管理端用户信息失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取小程序端微信用户（未登录返回 null）
     */
    public WxUser getWxUser(HttpServletRequest request) {
        String token = getToken(request);
        if (oConvertUtils.isEmpty(token)) {
            return null;
        }
        //update-begin---author:admin ---date:2026-08-17 for:【Android迁移】JWT 增加 userId claim 兼容旧 token---
        // 双 claim 兼容解析：优先 userId claim（新手机号登录 token），未命中则回退 openid claim（旧微信 token）
        String userId = HomeaiJwtUtil.getUserId(token);
        if (oConvertUtils.isNotEmpty(userId)) {
            WxUser wxUser = wxUserService.getById(userId);
            if (wxUser != null) {
                return wxUser;
            }
        }
        String openid = HomeaiJwtUtil.getOpenid(token);
        if (oConvertUtils.isEmpty(openid)) {
            return null;
        }
        return wxUserService.getByOpenid(openid);
        //update-end---author:admin ---date:2026-08-17 for:【Android迁移】JWT 增加 userId claim 兼容旧 token---
    }

    //update-begin---author:cursor---date:2026-08-20---for:【Android体验】业务接口按主键解析手机号 JWT-----------
    /**
     * 仅解析 HomeAI APP/小程序用户 ID（不含管理端账号）。
     * 手机号登录 JWT 的 openid claim 可能是主键 UUID，库内 openid 是 phone_*，必须先 getById。
     */
    public String getWxUserId(HttpServletRequest request) {
        WxUser wxUser = getWxUser(request);
        return wxUser != null ? wxUser.getId() : null;
    }
    //update-end---author:cursor---date:2026-08-20---for:【Android体验】业务接口按主键解析手机号 JWT-----------

    /**
     * 获取当前操作人 ID：
     * 管理端返回系统用户 ID，APP/小程序返回 WxUser 主键；均未登录返回 null
     */
    public String getCurrentUserId(HttpServletRequest request) {
        LoginUser loginUser = getConsoleUser(request);
        if (loginUser != null) {
            return loginUser.getId();
        }
        return getWxUserId(request);
    }

    //update-begin---author:cursor---date:2026-08-20---for:【Android体验】退出登录作废 Redis token-----------
    /**
     * 作废 APP token：同时删 userId 与 openid 两把 Redis 钥匙（手机号登录与微信登录各用一把）
     */
    public void invalidateWxUserTokens(WxUser user) {
        if (user == null) {
            return;
        }
        if (oConvertUtils.isNotEmpty(user.getId())) {
            redisUtil.del("homeai_token:" + user.getId());
            redisUtil.del("homeai_refresh:" + user.getId());
        }
        if (oConvertUtils.isNotEmpty(user.getOpenid())) {
            redisUtil.del("homeai_token:" + user.getOpenid());
            redisUtil.del("homeai_refresh:" + user.getOpenid());
        }
    }
    //update-end---author:cursor---date:2026-08-20---for:【Android体验】退出登录作废 Redis token-----------
}
