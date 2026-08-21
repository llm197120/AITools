package org.jeecg.modules.homeai.config;

import org.jeecg.common.util.filter.StrAttackFilter;
import org.jeecg.common.util.oss.OssBootUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OSS 私有上传 objectKey：不能把文件名里的点和下划线清掉。
 */
class OssPrivateObjectKeyTest {

    @Test
    void filterFullKeyWouldStripExtension() {
        String key = "homeai/recipe/covers/1787294773016_d0a054c2.png";
        String filtered = StrAttackFilter.filter(key);
        assertEquals("homeai/recipe/covers/1787294773016d0a054c2png", filtered);
        assertFalse(filtered.endsWith(".png"));
    }

    @Test
    void sanitizeKeepsExtensionAndUnderscore() {
        String key = "homeai/recipe/covers/1787294773016_d0a054c2.png";
        String sanitized = OssBootUtil.sanitizePrivateObjectKey(key);
        assertEquals(key, sanitized);
        assertTrue(sanitized.endsWith(".png"));
        assertTrue(sanitized.contains("_"));
    }

    @Test
    void sanitizeStripsLeadingSlash() {
        assertEquals(
                "homeai/recipe/covers/a.jpg",
                OssBootUtil.sanitizePrivateObjectKey("/homeai/recipe/covers/a.jpg"));
    }

    @Test
    void connectionResetIsTransient() {
        assertTrue(OssBootUtil.isTransientNetworkFailure(new java.net.SocketException("Connection reset by peer")));
        assertFalse(OssBootUtil.isTransientNetworkFailure(new IllegalArgumentException("bucket 不存在")));
    }
}
