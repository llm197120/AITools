package org.jeecg.modules.homeai.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.core.env.Environment;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * HomeAI 模块文件 URL 工具
 * 数据库统一保存文件绝对访问地址（http://host:port/context-path/upload/...），
 * 避免小程序端/管理端因相对地址无法访问的问题。
 */
@Slf4j
public class HomeaiFileUrlUtil {

    /** 禁止直接通过 /upload 静态地址访问的文件类型（防止存储型 XSS / 恶意脚本执行） */
    private static final String[] FORBIDDEN_EXTENSIONS = {
            "html", "htm", "shtml", "xhtml", "js", "jsp", "jspx", "asp", "aspx", "php",
            "sh", "bat", "cmd", "exe", "msi", "dll", "jar", "war", "svg"
    };

    /**
     * 将相对访问地址（/upload/...）转为绝对访问地址（http://host:port/context-path/upload/...）
     * 已为绝对地址时原样返回；无法获取请求上下文时回退为配置地址
     */
    public static String toAbsoluteUrl(String relativeUrl) {
        if (oConvertUtils.isEmpty(relativeUrl)) {
            return relativeUrl;
        }
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl;
        }
        if (!relativeUrl.startsWith("/")) {
            relativeUrl = "/" + relativeUrl;
        }
        String base = resolveBaseUrl();
        if (oConvertUtils.isEmpty(base)) {
            // 无请求上下文/配置时保留相对路径（单测与离线脚本常见），不必告警
            log.debug("无法解析文件访问根地址，保留相对地址: {}", relativeUrl);
            return relativeUrl;
        }
        return trimTrailingSlash(base) + relativeUrl;
    }

    /**
     * 从绝对地址或相对地址中提取相对访问路径（/upload/...）
     * 用于根据 fileUrl 定位物理存储目录
     */
    public static String toRelativeUrl(String url) {
        if (oConvertUtils.isEmpty(url)) {
            return url;
        }
        int idx = url.indexOf("/upload");
        if (idx >= 0) {
            return url.substring(idx);
        }
        return url;
    }

    /**
     * 黑名单校验（危险扩展名一律禁止）
     */
    public static boolean passBlacklist(String extension) {
        if (oConvertUtils.isEmpty(extension)) {
            return false;
        }
        String ext = extension.toLowerCase();
        for (String forbidden : FORBIDDEN_EXTENSIONS) {
            if (forbidden.equals(ext)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @deprecated 请使用 {@link org.jeecg.modules.homeai.config.service.IHomeaiFileWhitelistService#isAllowedExtension(String)}
     */
    @Deprecated
    public static boolean isAllowedUploadExtension(String extension) {
        return passBlacklist(extension);
    }

    private static String resolveBaseUrl() {
        //update-begin---author:admin ---date:2026-08-04  for：异步线程无请求上下文时回退配置地址-----------
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String domain = SpringContextUtils.getDomain();
                String contextPath = request.getContextPath();
                return joinBase(domain, contextPath);
            }
        } catch (Exception e) {
            log.debug("从请求上下文解析文件根地址失败: {}", e.getMessage());
        }
        return resolveBaseUrlFromConfig();
        //update-end---author:admin ---date:2026-08-04  for：异步线程无请求上下文时回退配置地址-----------
    }

    private static String resolveBaseUrlFromConfig() {
        try {
            if (SpringContextUtils.getApplicationContext() == null) {
                return null;
            }
            Environment env = SpringContextUtils.getApplicationContext().getEnvironment();
            String configured = env.getProperty("homeai.file.base-url");
            if (oConvertUtils.isNotEmpty(configured)) {
                return trimTrailingSlash(configured.trim());
            }
            String scheme = oConvertUtils.getString(env.getProperty("homeai.file.scheme"), "http");
            String host = oConvertUtils.getString(env.getProperty("homeai.file.host"), "127.0.0.1");
            String port = oConvertUtils.getString(env.getProperty("server.port"), "8080");
            String contextPath = oConvertUtils.getString(env.getProperty("server.servlet.context-path"), "");
            StringBuilder base = new StringBuilder(scheme).append("://").append(host);
            int portNum = Integer.parseInt(port);
            if (portNum != 80 && portNum != 443) {
                base.append(":").append(port);
            }
            if (oConvertUtils.isNotEmpty(contextPath)) {
                base.append(contextPath);
            }
            return trimTrailingSlash(base.toString());
        } catch (Exception e) {
            log.warn("从配置解析文件根地址失败: {}", e.getMessage());
            return null;
        }
    }

    private static String joinBase(String domain, String contextPath) {
        String base = domain + (oConvertUtils.isEmpty(contextPath) ? "" : contextPath);
        return trimTrailingSlash(base);
    }

    private static String trimTrailingSlash(String value) {
        if (oConvertUtils.isEmpty(value)) {
            return value;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
