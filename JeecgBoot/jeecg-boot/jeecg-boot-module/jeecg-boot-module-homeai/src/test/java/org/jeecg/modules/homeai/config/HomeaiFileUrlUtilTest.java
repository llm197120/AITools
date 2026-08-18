package org.jeecg.modules.homeai.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件 URL 工具纯逻辑测试
 *
 * <p>覆盖 HomeaiFileUrlUtil 的绝对/相对地址转换与危险扩展名黑名单校验。
 * 测试环境无 Spring 上下文，resolveBaseUrl 返回 null 时绝对地址原样返回。
 * 无 Spring 上下文、无 Mockito。</p>
 */
class HomeaiFileUrlUtilTest {

    // ---------- toAbsoluteUrl ----------

    @Test
    void nullUrlReturnsNull() {
        // null → 原样返回 null
        assertNull(HomeaiFileUrlUtil.toAbsoluteUrl(null));
    }

    @Test
    void absoluteHttpUrlUnchanged() {
        // 已是 http/https 绝对地址 → 原样返回
        assertEquals("http://x/y", HomeaiFileUrlUtil.toAbsoluteUrl("http://x/y"));
        assertEquals("https://x/y", HomeaiFileUrlUtil.toAbsoluteUrl("https://x/y"));
    }

    @Test
    void relativeUrlKeptWhenNoBaseResolvable() {
        // 测试环境无 Spring 上下文，resolveBaseUrl 返回 null → 原样返回相对地址
        assertEquals("/upload/a.jpg", HomeaiFileUrlUtil.toAbsoluteUrl("/upload/a.jpg"));
    }

    // ---------- toRelativeUrl ----------

    @Test
    void absoluteUrlReducedToUploadPath() {
        // 绝对地址 → 提取 /upload 起的相对路径
        assertEquals("/upload/a.jpg", HomeaiFileUrlUtil.toRelativeUrl("http://host:8080/jeecg-boot/upload/a.jpg"));
    }

    @Test
    void relativeUrlUnchanged() {
        // 已是相对地址 → 原样返回
        assertEquals("/upload/a.jpg", HomeaiFileUrlUtil.toRelativeUrl("/upload/a.jpg"));
    }

    @Test
    void urlWithoutUploadSegmentUnchanged() {
        // 不含 /upload 段 → 原样返回
        assertEquals("abc", HomeaiFileUrlUtil.toRelativeUrl("abc"));
    }

    @Test
    void nullUrlReturnsNullForRelative() {
        // null → 原样返回 null
        assertNull(HomeaiFileUrlUtil.toRelativeUrl(null));
    }

    // ---------- passBlacklist ----------

    @Test
    void emptyExtensionRejected() {
        // null / 空串 → 不通过
        assertFalse(HomeaiFileUrlUtil.passBlacklist(null));
        assertFalse(HomeaiFileUrlUtil.passBlacklist(""));
    }

    @Test
    void forbiddenExtensionsRejected() {
        // 危险扩展名（大小写不敏感）→ 不通过
        assertFalse(HomeaiFileUrlUtil.passBlacklist("html"));
        assertFalse(HomeaiFileUrlUtil.passBlacklist("js"));
        assertFalse(HomeaiFileUrlUtil.passBlacklist("exe"));
        assertFalse(HomeaiFileUrlUtil.passBlacklist("svg"));
        assertFalse(HomeaiFileUrlUtil.passBlacklist("HTML"));
    }

    @Test
    void safeExtensionsPass() {
        // 安全扩展名 → 通过
        assertTrue(HomeaiFileUrlUtil.passBlacklist("jpg"));
        assertTrue(HomeaiFileUrlUtil.passBlacklist("png"));
        assertTrue(HomeaiFileUrlUtil.passBlacklist("pdf"));
    }
}