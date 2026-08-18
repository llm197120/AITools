package org.jeecg.modules.homeai.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.util.PasswordUtil;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.HomeaiJwtUtil;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.service.IWxUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 手机号密码认证接口（Android 迁移）
 * 注册/登录后签发 JWT，phone 用户以 userId 作为 JWT subject（无微信 openid）
 */
//update-begin---author:admin ---date:2026-08-17 for:【Android迁移】手机号密码登录接口---
@Slf4j
@RestController
@RequestMapping("/homeai/auth")
public class HomeaiAuthController {

    @Autowired
    private IWxUserService wxUserService;

    @Autowired
    private RedisUtil redisUtil;

    /** JWT 签名密钥 */
    @Value("${homeai.jwt.secret:homeai-default-secret}")
    private String jwtSecret;

    /** Token 在 Redis 中的缓存前缀 */
    private static final String PREFIX_USER_TOKEN = "homeai_token:";
    private static final String PREFIX_REFRESH_TOKEN = "homeai_refresh:";

    /**
     * 手机号密码注册
     */
    @Operation(summary = "手机号密码注册")
    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterDTO dto) {
        try {
            // 1. 校验手机号格式
            if (dto.getPhone() == null || !dto.getPhone().matches("^1[3-9]\\d{9}$")) {
                return Result.error("手机号格式不正确");
            }
            // 2. 校验密码长度
            if (dto.getPassword() == null || dto.getPassword().length() < 6) {
                return Result.error("密码至少 6 位");
            }
            // 3. 手机号唯一性校验
            WxUser existUser = wxUserService.getByPhone(dto.getPhone());
            if (existUser != null) {
                return Result.error("该手机号已注册");
            }
            // 4. 创建用户（per-user 盐 + PBE 加密）
            String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            WxUser user = new WxUser();
            user.setPhone(dto.getPhone());
            user.setLoginType("phone");
            user.setNickname(oConvertUtils.isEmpty(dto.getNickname()) ? "用户" : dto.getNickname());
            user.setStatus(CommonConstant.STATUS_1);
            user.setSalt(salt);
            user.setPassword(PasswordUtil.encrypt(dto.getPassword(), PasswordUtil.SALT, salt));
            user.setCreateTime(new Date());
            wxUserService.save(user);
            log.info("手机号注册成功: phone={}", dto.getPhone());

            // 5. 签发 JWT Token（phone 用户以 userId 作为 JWT subject）
            String token = HomeaiJwtUtil.sign(user.getId(), jwtSecret, "APP");
            String refreshToken = HomeaiJwtUtil.signRefresh(user.getId(), jwtSecret);

            // 6. Token 存入 Redis（30天 / 60天）
            redisUtil.set(PREFIX_USER_TOKEN + user.getId(), token, 30 * 24 * 60 * 60);
            redisUtil.set(PREFIX_REFRESH_TOKEN + user.getId(), refreshToken, 60 * 24 * 60 * 60);

            // 7. 组装返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("refreshToken", refreshToken);
            result.put("isNewUser", true);
            result.put("userInfo", user);
            return Result.OK(result);
        } catch (Exception e) {
            log.error("手机号注册失败", e);
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    /**
     * 手机号密码登录
     */
    @Operation(summary = "手机号密码登录")
    @PostMapping("/login/password")
    public Result<?> loginByPassword(@RequestBody LoginDTO dto) {
        try {
            // 1. 校验手机号格式
            if (dto.getPhone() == null || !dto.getPhone().matches("^1[3-9]\\d{9}$")) {
                return Result.error("手机号格式不正确");
            }
            // 2. 查询用户
            WxUser user = wxUserService.getByPhone(dto.getPhone());
            if (user == null) {
                return Result.error("手机号未注册");
            }
            // 3. 密码校验（per-user 盐）
            String encrypted = PasswordUtil.encrypt(dto.getPassword(), PasswordUtil.SALT, user.getSalt());
            if (!encrypted.equals(user.getPassword())) {
                return Result.error("密码错误");
            }
            // 4. 补齐登录方式并更新最后登录时间
            if (oConvertUtils.isEmpty(user.getLoginType())) {
                user.setLoginType("phone");
            }
            user.setLastLoginTime(new Date());
            wxUserService.updateById(user);

            // 5. 签发 JWT Token（phone 用户以 userId 作为 JWT subject）
            String token = HomeaiJwtUtil.sign(user.getId(), jwtSecret, "APP");
            String refreshToken = HomeaiJwtUtil.signRefresh(user.getId(), jwtSecret);

            // 6. Token 存入 Redis（30天 / 60天）
            redisUtil.set(PREFIX_USER_TOKEN + user.getId(), token, 30 * 24 * 60 * 60);
            redisUtil.set(PREFIX_REFRESH_TOKEN + user.getId(), refreshToken, 60 * 24 * 60 * 60);

            // 7. 组装返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("refreshToken", refreshToken);
            result.put("isNewUser", false);
            result.put("userInfo", user);
            return Result.OK(result);
        } catch (Exception e) {
            log.error("手机号登录失败", e);
            return Result.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 注册请求体
     */
    @Data
    public static class RegisterDTO {
        private String phone;
        private String password;
        private String nickname;
    }

    /**
     * 密码登录请求体
     */
    @Data
    public static class LoginDTO {
        private String phone;
        private String password;
    }
}
//update-end---author:admin ---date:2026-08-17 for:【Android迁移】手机号密码登录接口---
