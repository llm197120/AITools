package org.jeecg.modules.homeai.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.common.util.oConvertUtils;

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
     * 已为绝对地址时原样返回；无法获取请求上下文时回退为相对地址
     */
    public static String toAbsoluteUrl(String relativeUrl) {
        if (oConvertUtils.isEmpty(relativeUrl)) {
            return relativeUrl;
        }
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl;
        }
        try {
            HttpServletRequest request = SpringContextUtils.getHttpServletRequest();
            String domain = SpringContextUtils.getDomain();
            String contextPath = request.getContextPath();
            String base = domain + (oConvertUtils.isEmpty(contextPath) ? "" : contextPath);
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            if (!relativeUrl.startsWith("/")) {
                relativeUrl = "/" + relativeUrl;
            }
            return base + relativeUrl;
        } catch (Exception e) {
            log.warn("转换文件绝对地址失败，保留相对地址: {}", relativeUrl, e);
            return relativeUrl;
        }
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
     * 判断上传文件扩展名是否允许（黑名单校验，扩展名统一转小写）
     */
    public static boolean isAllowedUploadExtension(String extension) {
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
}
