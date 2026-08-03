package org.jeecg.modules.homeai.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * HomeAI 模块统一鉴权过滤器。
 * <p>
 * 背景：/homeai/** 在 Shiro 中配置为 anon（小程序端使用独立 JWT 认证），
 * 导致管理端接口（用户/家庭/账单/计划/菜谱/学习/存储/AI 密钥等 CRUD、导入导出、回收站）
 * 处于完全匿名可访问状态，任何人都能读取、修改甚至物理删除业务数据。
 * </p>
 * <p>
 * 本过滤器对 /homeai/** 请求按三类处理：
 * 1. 公开接口（登录、刷新 token、CORS 预检）直接放行；
 * 2. 管理端接口：必须携带有效的 JeecgBoot 控制台 JWT；
 * 3. 小程序接口：必须携带有效的 HomeAI JWT（openid 声明，且与 Redis 中的 token 一致）
 *    或控制台 JWT（管理端页面也会访问部分共享接口）。
 * </p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class HomeaiAuthFilter implements Filter {

    /** 完全公开的接口（无需任何 token） */
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/homeai/user/login",
            "/homeai/user/refresh-token"
    );

    /** 管理端专属资源前缀（必须控制台 JWT） */
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
            "/homeai/recipe/add",
            "/homeai/recipe/exportXls",
            "/homeai/recipe/importExcel",
            "/homeai/recipe/recycleBin",
            "/homeai/recipe/moveToRecycleBin",
            "/homeai/recipe/restore",
            "/homeai/recipe/deletePermanently",
            "/homeai/learn/addMaterial",
            "/homeai/learn/exportXls",
            "/homeai/learn/importExcel",
            "/homeai/learn/recycleBin",
            "/homeai/learn/moveToRecycleBin",
            "/homeai/learn/restore",
            "/homeai/learn/deletePermanently",
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
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // 提取去除 context-path 后的路径
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (oConvertUtils.isNotEmpty(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        // 仅处理 HomeAI 模块请求
        if (!path.startsWith("/homeai/") && !path.equals("/homeai")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // CORS 预检直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // 公开接口放行
        if (PUBLIC_PATHS.contains(path)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String token = securityUtil.getToken(request);
        boolean isAdminPath = isAdminPath(path, request.getMethod());

        if (oConvertUtils.isEmpty(token)) {
            writeUnauthorized(response, "未登录或登录已过期");
            return;
        }

        // 管理端接口：必须为有效的控制台 JWT
        if (isAdminPath) {
            if (!isConsoleTokenValid(token) || !loginConsoleSubject(servletRequest, servletResponse, token)) {
                writeUnauthorized(response, "无权限访问，请使用管理端账号登录");
            } else {
                filterChain.doFilter(servletRequest, servletResponse);
            }
            return;
        }

        // 小程序接口：HomeAI JWT 或控制台 JWT 任一有效即可
        boolean consoleOk = isConsoleTokenValid(token);
        boolean appOk = isHomeaiAppTokenValid(token);
        if (consoleOk || appOk) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            writeUnauthorized(response, "Token 无效或已过期，请重新登录");
        }
    }

    /**
     * 校验 JeecgBoot 控制台 JWT（与 Shiro JwtFilter 的 Realm 校验逻辑一致）
     */
    private boolean isConsoleTokenValid(String token) {
        try {
            LoginUser loginUser = shiroRealm.checkUserTokenIsEffect(token);
            return loginUser != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将控制台用户登录到 Shiro Subject（与 JwtFilter 一致），
     * 使 @RequiresPermissions / @AutoLog 等注解机制生效
     */
    private boolean loginConsoleSubject(ServletRequest request, ServletResponse response, String token) {
        try {
            SecurityUtils.getSubject().login(new JwtToken(token));
            return true;
        } catch (Exception e) {
            log.warn("HomeAI 管理端 Subject 登录失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 校验 HomeAI 小程序 JWT：签名有效且与 Redis 中缓存的一致
     */
    private boolean isHomeaiAppTokenValid(String token) {
        try {
            String openid = HomeaiJwtUtil.getOpenid(token);
            if (oConvertUtils.isEmpty(openid)) {
                return false;
            }
            String cached = (String) redisUtil.get(WxUserTokenConstants.PREFIX_USER_TOKEN + openid);
            return cached != null && cached.equals(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否为管理端专属接口
     */
    private boolean isAdminPath(String path, String method) {
        String m = method == null ? "" : method.toUpperCase();

        // 前缀匹配的管理端接口
        for (String prefix : ADMIN_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }

        // 用户管理：/{id}、/{id}/family、/{id}/status（GET/PUT/DELETE），排除小程序端 info/login/refresh-token
        if (path.startsWith("/homeai/user/")) {
            String rest = path.substring("/homeai/user/".length());
            if (oConvertUtils.isNotEmpty(rest)) {
                String first = rest.split("/")[0];
                if (!first.equals("info") && !first.equals("login") && !first.equals("refresh-token")) {
                    return true;
                }
            }
        }
        // 用户管理：POST /homeai/user（新增）
        if (path.equals("/homeai/user") && "POST".equals(m)) {
            return true;
        }

        // 各模块管理端 {id} 编辑接口（PUT）
        if ("PUT".equals(m)) {
            if (path.matches("/homeai/family/[^/]+")) {
                return true;
            }
            if (path.matches("/homeai/bill/[^/]+")
                    && !path.equals("/homeai/bill/entry") && !path.equals("/homeai/bill/category")) {
                return true;
            }
            if (path.matches("/homeai/plan/[^/]+")
                    && !path.startsWith("/homeai/plan/instance/")) {
                return true;
            }
            if (path.matches("/homeai/recipe/[^/]+")
                    && !path.matches("/homeai/recipe/[^/]+/video")) {
                return true;
            }
            if (path.matches("/homeai/learn/material/[^/]+")) {
                return true;
            }
        }

        // 存储转换规则/模板：管理端 CRUD 需控制台 token；小程序只读接口（targets/enabled）放行
        if (path.startsWith("/homeai/storage/rule/")) {
            String rest = path.substring("/homeai/storage/rule/".length());
            if (!rest.equals("targets")) {
                return true;
            }
        }
        if (path.equals("/homeai/storage/rule") && ("POST".equals(m) || "PUT".equals(m))) {
            return true;
        }
        if (path.startsWith("/homeai/storage/template/")) {
            String rest = path.substring("/homeai/storage/template/".length());
            if (!rest.equals("enabled")) {
                return true;
            }
        }
        if (path.equals("/homeai/storage/template") && ("POST".equals(m) || "PUT".equals(m))) {
            return true;
        }
        return false;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"success\":false,\"code\":401,\"message\":\"" + message + "\"}");
    }

    /**
     * HomeAI token 在 Redis 中的缓存前缀（与 WxUserServiceImpl 保持一致）
     */
    private static final class WxUserTokenConstants {
        private static final String PREFIX_USER_TOKEN = "homeai_token:";
    }
}
