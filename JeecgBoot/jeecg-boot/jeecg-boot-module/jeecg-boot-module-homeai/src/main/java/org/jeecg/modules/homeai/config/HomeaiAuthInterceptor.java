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
            "/homeai/user/refresh-token"
    );

    private static final List<String> ADMIN_PREFIXES = Arrays.asList(
            "/homeai/user/list",
            "/homeai/user/exportXls",
            "/homeai/user/exportTemplate",
            "/homeai/user/importExcel",
            "/homeai/user/recycleBin",
            "/homeai/user/moveToRecycleBin",
            "/homeai/user/restore",
            "/homeai/user/deletePermanently",
            "/homeai/family/list",
            "/homeai/family/admin",
            "/homeai/family/add",
            "/homeai/family/exportXls",
            "/homeai/family/exportTemplate",
            "/homeai/family/importExcel",
            "/homeai/family/recycleBin",
            "/homeai/family/moveToRecycleBin",
            "/homeai/family/restore",
            "/homeai/family/deletePermanently",
            "/homeai/bill/list",
            "/homeai/bill/add",
            "/homeai/bill/exportXls",
            "/homeai/bill/importExcel",
            "/homeai/bill/recycleBin",
            "/homeai/bill/moveToRecycleBin",
            "/homeai/bill/restore",
            "/homeai/bill/deletePermanently",
            "/homeai/plan/list",
            "/homeai/plan/add",
            "/homeai/plan/exportXls",
            "/homeai/plan/importExcel",
            "/homeai/plan/recycleBin",
            "/homeai/plan/moveToRecycleBin",
            "/homeai/plan/restore",
            "/homeai/plan/deletePermanently",
            "/homeai/plan/category-list",
            "/homeai/plan/category",
            "/homeai/plan/admin/completion",
            "/homeai/plan/admin/calendar",
            "/homeai/plan/admin/date",
            "/homeai/recipe/add",
            "/homeai/recipe/exportXls",
            "/homeai/recipe/importExcel",
            "/homeai/recipe/recycleBin",
            "/homeai/recipe/moveToRecycleBin",
            "/homeai/recipe/restore",
            "/homeai/recipe/deletePermanently",
            "/homeai/recipe/category/list",
            "/homeai/learn/addMaterial",
            "/homeai/learn/exportXls",
            "/homeai/learn/importExcel",
            "/homeai/learn/recycleBin",
            "/homeai/learn/moveToRecycleBin",
            "/homeai/learn/restore",
            "/homeai/learn/deletePermanently",
            "/homeai/learn/upload",
            "/homeai/learn/category/list",
            "/homeai/ai/key-config",
            "/homeai/ai/conversations/list",
            "/homeai/storage/folder-list",
            "/homeai/storage/file-list",
            "/homeai/storage/office/list"
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

        boolean isAdminPath = isAdminPath(path, request.getMethod());
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

    private boolean isAdminPath(String path, String method) {
        String m = method == null ? "" : method.toUpperCase();
        for (String prefix : ADMIN_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }

        if (path.startsWith("/homeai/user/")) {
            String rest = path.substring("/homeai/user/".length());
            if (oConvertUtils.isNotEmpty(rest)) {
                String first = rest.split("/")[0];
                if (!first.equals("info") && !first.equals("login") && !first.equals("refresh-token")) {
                    return true;
                }
            }
        }
        if (path.equals("/homeai/user") && "POST".equals(m)) {
            return true;
        }

        if ("PUT".equals(m)) {
            if (path.matches("/homeai/family/[^/]+")) return true;
            if (path.matches("/homeai/bill/[^/]+")
                    && !path.equals("/homeai/bill/entry") && !path.equals("/homeai/bill/category")) return true;
            if (path.matches("/homeai/plan/[^/]+") && !path.startsWith("/homeai/plan/instance/")) return true;
            if (path.matches("/homeai/recipe/[^/]+") && !path.matches("/homeai/recipe/[^/]+/video")) return true;
            if (path.matches("/homeai/learn/material/[^/]+")) return true;
        }

        if (path.startsWith("/homeai/storage/rule/")) {
            String rest = path.substring("/homeai/storage/rule/".length());
            if (!rest.equals("targets")) return true;
        }
        if (path.equals("/homeai/storage/rule") && ("POST".equals(m) || "PUT".equals(m))) return true;
        if (path.startsWith("/homeai/storage/template/")) {
            String rest = path.substring("/homeai/storage/template/".length());
            if (!rest.equals("enabled")) return true;
        }
        if (path.equals("/homeai/storage/template") && ("POST".equals(m) || "PUT".equals(m))) return true;
        if (path.startsWith("/homeai/recipe/category/")) {
            String rest = path.substring("/homeai/recipe/category/".length());
            if (!rest.equals("all")) return true;
        }
        if (path.equals("/homeai/recipe/category") && ("POST".equals(m) || "PUT".equals(m))) return true;
        if (path.startsWith("/homeai/learn/category/")) {
            String rest = path.substring("/homeai/learn/category/".length());
            if (!rest.equals("all")) return true;
        }
        if (path.equals("/homeai/learn/category") && ("POST".equals(m) || "PUT".equals(m))) return true;
        if (path.equals("/homeai/config/file-whitelist") && "PUT".equals(m)) return true;
        return false;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"success\":false,\"code\":401,\"message\":\"" + message + "\"}");
    }
}
