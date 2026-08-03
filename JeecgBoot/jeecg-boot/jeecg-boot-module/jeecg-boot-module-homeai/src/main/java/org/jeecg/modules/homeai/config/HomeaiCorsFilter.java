package org.jeecg.modules.homeai.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * HomeAI 模块 CORS 过滤器。
 * <p>
 * 背景：/homeai/** 在 Shiro 中配置为 anon（走 HomeAI 独立 JWT 认证），绕过了 JwtFilter，
 * 而全局 CorsFilter(FilterRegistrationBean) 在当前 Spring Boot 4 环境下未生效，
 * 导致管理端跨域上传（defHttp.uploadFile 直连 uploadUrl）时浏览器拦截响应，报“网络异常”。
 * <p>
 * 本过滤器通过 @Component 自动注册（与 Shiro AbstractShiroFilter 相同机制），
 * 仅对 /homeai/** 请求补充 CORS 响应头，并对 OPTIONS 预检直接返回 200。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HomeaiCorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // getRequestURI 包含 context-path（如 /jeecg-boot/homeai/...），需去除 context-path 后再匹配
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        // 仅处理 HomeAI 模块请求，其余放行，避免影响其他模块
        if (!path.startsWith("/homeai/") && !path.equals("/homeai")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && !origin.isEmpty()) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE,OPTIONS,PATCH");
            String requestHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    requestHeaders != null && !requestHeaders.isEmpty() ? requestHeaders : "*");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
        }

        // 跨域预检请求直接返回 200
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
