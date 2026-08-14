package org.jeecg.modules.homeai.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * HomeAI 模块 JWT 工具
 * 使用微信 openid 作为 JWT 主题，区别于管理端的 username 认证
 */
@Slf4j
@Component
public class HomeaiJwtUtil {

    /** 小程序端 Token 有效期 30 天 */
    private static final long APP_EXPIRE_TIME = 30 * 24 * 60 * 60 * 1000L;

    /** 管理端 Token 有效期 7 天 */
    private static final long PC_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;

    /** RefreshToken 有效期 60 天 */
    private static final long REFRESH_EXPIRE_TIME = 60 * 24 * 60 * 60 * 1000L;

    /** JWT 签名密钥（从配置文件注入） */
    @Value("${homeai.jwt.secret:homeai-default-secret}")
    private String jwtSecretConfig;

    private static String JWT_SECRET;

    @PostConstruct
    public void init() {
        JWT_SECRET = jwtSecretConfig;
        //update-begin---author:cursor ---date:2026-08-13 for：【安全加固】默认密钥告警，避免上线误用公开默认密钥-----------
        if ("homeai-default-secret".equals(JWT_SECRET) || oConvertUtils.isEmpty(JWT_SECRET)) {
            log.warn("【安全警告】homeai.jwt.secret 使用默认值/为空，JWT 可被伪造！生产环境务必在配置中设置强随机密钥。");
        } else {
            log.info("HomeaiJwtUtil 已初始化，JWT 签名验证已启用");
        }
        //update-end---author:cursor ---date:2026-08-13 for：【安全加固】默认密钥告警-----------
    }

    /**
     * 生成签名（小程序端）
     * @param openid   微信 openid
     * @param secret   签名密钥
     * @return JWT token
     */
    public static String sign(String openid, String secret) {
        return sign(openid, secret, "APP");
    }

    /**
     * 生成签名
     * @param openid      微信 openid
     * @param secret      签名密钥
     * @param clientType  客户端类型 APP/PC
     * @return JWT token
     */
    public static String sign(String openid, String secret, String clientType) {
        long expireTime = "APP".equalsIgnoreCase(clientType) ? APP_EXPIRE_TIME : PC_EXPIRE_TIME;
        Date date = new Date(System.currentTimeMillis() + expireTime);
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withClaim("openid", openid)
                .withClaim("clientType", clientType)
                .withExpiresAt(date)
                .sign(algorithm);
    }

    /**
     * 生成 RefreshToken
     * @param openid  微信 openid
     * @param secret  签名密钥
     * @return refresh token
     */
    public static String signRefresh(String openid, String secret) {
        Date date = new Date(System.currentTimeMillis() + REFRESH_EXPIRE_TIME);
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withClaim("openid", openid)
                .withClaim("type", "refresh")
                .withExpiresAt(date)
                .sign(algorithm);
    }

    /**
     * 从 token 中验证签名并获取 openid
     * @param token JWT token
     * @return openid，验证失败返回 null
     */
    public static String getOpenid(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
            return jwt.getClaim("openid").asString();
        } catch (JWTVerificationException e) {
            log.error("Token 签名验证失败: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Token 解析异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 HttpServletRequest 中提取 openid
     * @param request HTTP请求
     * @return openid，如果未登录返回 null
     */
    public static String getOpenidFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        if (request == null) return null;
        String token = request.getHeader("X-Access-Token");
        return getOpenid(token);
    }
}
