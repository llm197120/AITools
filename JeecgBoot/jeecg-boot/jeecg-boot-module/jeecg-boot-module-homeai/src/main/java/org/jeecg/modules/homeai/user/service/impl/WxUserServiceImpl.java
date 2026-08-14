package org.jeecg.modules.homeai.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.homeai.config.HomeaiJwtUtil;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.mapper.WxUserMapper;
import org.jeecg.modules.homeai.user.service.IWxUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信用户 Service 实现
 */
@Slf4j
@Service
public class WxUserServiceImpl extends ServiceImpl<WxUserMapper, WxUser> implements IWxUserService {

    @Autowired
    private RedisUtil redisUtil;

    /** 微信小程序 appid */
    @Value("${homeai.wechat.appid:}")
    private String appid;

    /** 微信小程序 secret */
    @Value("${homeai.wechat.secret:}")
    private String secret;

    /** JWT 签名密钥 */
    @Value("${homeai.jwt.secret:homeai-default-secret}")
    private String jwtSecret;

    /** 当前激活的 Spring Profile（用于区分开发/生产环境） */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /** Token 在 Redis 中的缓存前缀 */
    private static final String PREFIX_USER_TOKEN = "homeai_token:";
    private static final String PREFIX_REFRESH_TOKEN = "homeai_refresh:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> login(String code) {
        // 1. 调用微信 API 换取 openid
        String openid = wechatCode2Session(code);

        // 2. 查询用户是否存在
        WxUser user = getByOpenid(openid);
        boolean isNewUser = false;

        if (user == null) {
            // 3. 新用户自动注册
            user = new WxUser();
            user.setOpenid(openid);
            user.setNickname("微信用户");
            user.setStatus(CommonConstant.STATUS_1);
            user.setCreateTime(new Date());
            save(user);
            isNewUser = true;
            log.info("新微信用户注册: openid={}", openid);
        }

        // 4. 更新最后登录时间
        user.setLastLoginTime(new Date());
        updateById(user);

        // 5. 签发 JWT Token
        String token = HomeaiJwtUtil.sign(openid, jwtSecret, "APP");
        String refreshToken = HomeaiJwtUtil.signRefresh(openid, jwtSecret);

        // 6. Token 存入 Redis
        redisUtil.set(PREFIX_USER_TOKEN + openid, token, 30 * 24 * 60 * 60);
        redisUtil.set(PREFIX_REFRESH_TOKEN + openid, refreshToken, 60 * 24 * 60 * 60);

        // 7. 组装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("isNewUser", isNewUser);
        result.put("userInfo", user);

        return result;
    }

    @Override
    public Map<String, Object> refreshToken(String refreshToken) {
        String openid = HomeaiJwtUtil.getOpenid(refreshToken);
        if (openid == null) {
            throw new RuntimeException("Token 无效");
        }

        // 校验 Redis 中的 refreshToken
        String cachedRefresh = (String) redisUtil.get(PREFIX_REFRESH_TOKEN + openid);
        if (cachedRefresh == null || !cachedRefresh.equals(refreshToken)) {
            throw new RuntimeException("RefreshToken 已过期");
        }

        // 签发新 Token
        String newToken = HomeaiJwtUtil.sign(openid, jwtSecret, "APP");
        String newRefreshToken = HomeaiJwtUtil.signRefresh(openid, jwtSecret);

        redisUtil.set(PREFIX_USER_TOKEN + openid, newToken, 30 * 24 * 60 * 60);
        redisUtil.set(PREFIX_REFRESH_TOKEN + openid, newRefreshToken, 60 * 24 * 60 * 60);

        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        result.put("refreshToken", newRefreshToken);
        return result;
    }

    @Override
    public WxUser getByOpenid(String openid) {
        LambdaQueryWrapper<WxUser> query = new LambdaQueryWrapper<>();
        query.eq(WxUser::getOpenid, openid);
        return getOne(query);
    }

    /**
     * 调用微信 code2Session 接口换取 openid
     * 开发环境未配置 appid/secret 时可 mock 返回（便于本地联调）
     */
    private String wechatCode2Session(String code) {
        if (appid.isEmpty() || secret.isEmpty()) {
            //update-begin---author:cursor ---date:2026-08-13 for：【安全加固】生产环境拒绝 mock 登录，避免任意 code 注册-----------
            String profile = activeProfile == null ? "" : activeProfile.toLowerCase();
            boolean devLike = profile.contains("dev") || profile.contains("test");
            if (!devLike) {
                log.error("当前环境 [{}] 未配置 homeai.wechat.appid/secret，已拒绝 mock 登录", activeProfile);
                throw new RuntimeException("服务端微信配置缺失，请联系管理员");
            }
            log.warn("微信配置未设置（dev 环境），使用 mock openid: mock_{}", code);
            //update-end---author:cursor ---date:2026-08-13 for：【安全加固】生产环境拒绝 mock 登录-----------
            return "mock_" + code;
        }
        // 调用微信官方 code2Session 接口
        try {
            String requestUrl = "https://api.weixin.qq.com/sns/jscode2session?appid="
                    + URLEncoder.encode(appid, StandardCharsets.UTF_8)
                    + "&secret=" + URLEncoder.encode(secret, StandardCharsets.UTF_8)
                    + "&js_code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&grant_type=authorization_code";
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            log.debug("微信 code2Session 响应: {}", responseBody);
            JSONObject json = JSONObject.parseObject(responseBody);
            String openid = json.getString("openid");
            if (openid == null || openid.isEmpty()) {
                String errMsg = json.getString("errmsg");
                log.error("微信登录失败: {}", responseBody);
                throw new RuntimeException("微信登录失败: " + errMsg);
            }
            return openid;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信 code2Session 接口异常", e);
            throw new RuntimeException("微信登录失败，请稍后重试", e);
        }
    }
}
