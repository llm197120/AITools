package org.jeecg.modules.homeai.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.config.shiro.JwtToken;
import org.jeecg.config.shiro.ShiroRealm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * HomeAI 模块统一鉴权拦截器。
 * <p>
 * 使用 MVC 拦截器而非 Servlet 过滤器：拦截器在 Shiro 过滤器链（AbstractShiroFilter）
 * 之后、Controller 之前执行，此时 {@code SecurityUtils.getSubject()} 返回的即请求绑定的
 * WebSubject，对其 login 后 @RequiresPermissions / @AutoLog 等 Shiro 注解机制才会真正生效。
 * </p>
 * <p>
 * 处理规则：
 * 1. 公开接口（登录、刷新 token、CORS 预检）直接放行；
 * 2. 管理端接口：必须携带有效控制台 JWT，并登录到 Shiro Subject；
 * 3. 小程序接口：HomeAI JWT（openid，Redis 一致）或控制台 JWT 任一有效。
 * </p>
 */
@Slf4j
@Component
public class HomeaiAuthInterceptor implements HandlerInterceptor {

    /** 完全公开的接口（无需任何 token） */
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/homeai/user/login",
            "/homeai/user/refresh-token",
            //update-begin---author:admin ---date:2026-08-12 for：【HomeAI】订阅模板公开配置-----------
            "/homeai/config/wechat-public",
            //update-end---author:admin ---date:2026-08-12 for：【HomeAI】订阅模板公开配置-----------
            //update-begin---author:admin ---date:2026-08-17 for:【Android迁移】手机号密码登录公开接口-----------
            "/homeai/auth/register",
            "/homeai/auth/login/password",
            //update-end---author:admin ---date:2026-08-17 for:【Android迁移】手机号密码登录公开接口-----------
            //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R69】APP 版本公开探测-----------
            "/homeai/app/version",
            //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R69】APP 版本公开探测-----------
            //update-begin---author:cursor---date:2026-08-31---for:【APP更新】APK 代理下载公开（所有用户需下载更新包）-----------
            "/homeai/app/version/package/download",
            //update-end---author:cursor---date:2026-08-31---for:【APP更新】APK 代理下载公开-----------
            //update-begin---author:cursor---date:2026-08-31---for:【APP离线】同步配置 App 公开拉取-----------
            "/homeai/config/sync"
            //update-end---author:cursor---date:2026-08-31---for:【APP离线】同步配置 App 公开拉取-----------
    );

    @Lazy
    @Autowired
    private ShiroRealm shiroRealm;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private HomeaiSecurityUtil securityUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (oConvertUtils.isNotEmpty(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        if (!path.startsWith("/homeai/") && !path.equals("/homeai")) {
            return true;
        }
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }

        String token = securityUtil.getToken(request);
        if (oConvertUtils.isEmpty(token)) {
            writeUnauthorized(response, "未登录或登录已过期");
            return false;
        }

        //update-begin---author:cursor---date:2026-08-22---for:【审查D】管理端路径判定抽出以便单测---
        boolean isAdminPath = HomeaiAdminPathUtil.isAdminPath(path, request.getMethod());
        //update-end---author:cursor---date:2026-08-22---for:【审查D】管理端路径判定抽出以便单测---
        boolean consoleOk = isConsoleTokenValid(token);

        if (isAdminPath) {
            if (consoleOk && loginConsoleSubject(token)) {
                return true;
            }
            writeUnauthorized(response, "无权限访问，请使用管理端账号登录");
            return false;
        }

        // 小程序接口：控制台 JWT 或 HomeAI JWT 任一有效
        if (consoleOk) {
            loginConsoleSubject(token);
            return true;
        }
        if (isHomeaiAppTokenValid(token)) {
            return true;
        }
        writeUnauthorized(response, "Token 无效或已过期，请重新登录");
        return false;
    }

    private boolean isConsoleTokenValid(String token) {
        try {
            LoginUser loginUser = shiroRealm.checkUserTokenIsEffect(token);
            return loginUser != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将控制台用户登录到当前请求绑定的 Shiro Subject（拦截器阶段即 Shiro 过滤器链之后）
     */
    private boolean loginConsoleSubject(String token) {
        try {
            SecurityUtils.getSubject().login(new JwtToken(token));
            return true;
        } catch (Exception e) {
            log.warn("HomeAI 管理端 Subject 登录失败: {}", e.getMessage());
            return false;
        }
    }

    private boolean isHomeaiAppTokenValid(String token) {
        try {
            //update-begin---author:cursor---date:2026-08-20---for:【Android登录】手机号 JWT 优先按 userId 校验 Redis token-----------
            String userId = HomeaiJwtUtil.getUserId(token);
            if (oConvertUtils.isNotEmpty(userId)) {
                String cachedByUserId = (String) redisUtil.get("homeai_token:" + userId);
                if (cachedByUserId != null && cachedByUserId.equals(token)) {
                    return true;
                }
            }
            //update-end---author:cursor---date:2026-08-20---for:【Android登录】手机号 JWT 优先按 userId 校验 Redis token-----------
            String openid = HomeaiJwtUtil.getOpenid(token);
            if (oConvertUtils.isEmpty(openid)) {
                return false;
            }
            String cached = (String) redisUtil.get("homeai_token:" + openid);
            return cached != null && cached.equals(token);
        } catch (Exception e) {
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"success\":false,\"code\":401,\"message\":\"" + message + "\"}");
    }
}
