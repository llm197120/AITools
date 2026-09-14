package org.jeecg.modules.homeai.config.util;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.system.util.XssUtils;

/**
 * 协议/隐私富文本：长度限制 + 存储型 XSS 过滤。
 */
public final class HomeaiDocHtmlUtil {

    public static final int MAX_CHARS = 200_000;

    private HomeaiDocHtmlUtil() {
    }

    public static String sanitize(String html) {
        String raw = html == null ? "" : html;
        if (raw.length() > MAX_CHARS) {
            throw new JeecgBootException("内容过长，最多 " + MAX_CHARS + " 个字符");
        }
        String cleaned = XssUtils.richTextXss(raw);
        return cleaned == null ? "" : cleaned;
    }
}
