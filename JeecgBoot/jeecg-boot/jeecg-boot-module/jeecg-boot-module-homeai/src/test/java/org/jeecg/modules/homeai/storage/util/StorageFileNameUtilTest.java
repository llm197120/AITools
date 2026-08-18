package org.jeecg.modules.homeai.storage.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资料存储文件名工具纯逻辑测试
 *
 * <p>覆盖 StorageFileNameUtil 的对象路径拼接、原始文件名清洗截断、
 * 微信临时路径判定与扩展名提取。无 Spring 上下文、无 Mockito。</p>
 */
class StorageFileNameUtilTest {

    // ---------- buildObjectKey ----------

    @Test
    void buildObjectKeyJoinsParts() {
        // 对象路径 = homeai/{userId}/{storedName}
        assertEquals("homeai/u1/abc.jpg", StorageFileNameUtil.buildObjectKey("u1", "abc.jpg"));
    }

    @Test
    void buildObjectKeyRejectsEmptyArgs() {
        // userId 或 storedName 为空 → IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> StorageFileNameUtil.buildObjectKey(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> StorageFileNameUtil.buildObjectKey("u1", null));
    }

    // ---------- sanitizeOriginalName ----------

    @Test
    void emptyNameDefaultsToUnknown() {
        // null / 空串 / 纯空白 → unknown
        assertEquals("unknown", StorageFileNameUtil.sanitizeOriginalName(null));
        assertEquals("unknown", StorageFileNameUtil.sanitizeOriginalName(""));
        assertEquals("unknown", StorageFileNameUtil.sanitizeOriginalName("   "));
    }

    @Test
    void pathTraversalStrippedToFileName() {
        // 反斜杠路径穿越 → 转正斜杠后取最后一段
        assertEquals("evil.jpg", StorageFileNameUtil.sanitizeOriginalName("..\\..\\evil.jpg"));
    }

    @Test
    void illegalCharsReplacedWithUnderscore() {
        // 非法字符 < > : * ? " | 替换为 _
        assertEquals("a_b_c_d_e_f_g_h", StorageFileNameUtil.sanitizeOriginalName("a<b>c:d*e?f\"g|h"));
    }

    @Test
    void controlCharsRemoved() {
        // 控制字符（\x00-\x1f）直接删除
        assertEquals("abc", StorageFileNameUtil.sanitizeOriginalName("a\u0000b\u001fc"));
    }

    @Test
    void longNameTruncatedKeepingExtension() {
        // 超长文件名保留扩展名截断到 300
        String longName = "x".repeat(350) + ".jpg";
        String result = StorageFileNameUtil.sanitizeOriginalName(longName);
        assertEquals(300, result.length());
        assertTrue(result.endsWith(".jpg"));
    }

    @Test
    void longNameWithoutExtensionTruncated() {
        // 无扩展名超长文件名直接截断到 300
        String longName = "x".repeat(350);
        assertEquals(300, StorageFileNameUtil.sanitizeOriginalName(longName).length());
    }

    @Test
    void allSlashesCleanedToUnknown() {
        // 清洗后为空 → unknown
        assertEquals("unknown", StorageFileNameUtil.sanitizeOriginalName("///"));
    }

    // ---------- isTempUploadName ----------

    @Test
    void nullNameIsTemp() {
        // null → 视为临时名
        assertTrue(StorageFileNameUtil.isTempUploadName(null));
    }

    @Test
    void tempPrefixNamesDetected() {
        // tmp/temp 前缀、wxfile 路径、纯数字图片名、大小写不敏感
        assertTrue(StorageFileNameUtil.isTempUploadName("tmp_xxx.jpg"));
        assertTrue(StorageFileNameUtil.isTempUploadName("temp_xxx"));
        assertTrue(StorageFileNameUtil.isTempUploadName("wxfile://tmp_xxx"));
        assertTrue(StorageFileNameUtil.isTempUploadName("123456.jpg"));
        assertTrue(StorageFileNameUtil.isTempUploadName("TMP_xxx"));
    }

    @Test
    void normalNamesNotTemp() {
        // 非临时命名规则 → false
        assertFalse(StorageFileNameUtil.isTempUploadName("123456.xyz"));
        assertFalse(StorageFileNameUtil.isTempUploadName("recipe.jpg"));
    }

    // ---------- extensionOf ----------

    @Test
    void extensionExtractedLowercased() {
        // 提取扩展名并转小写
        assertEquals("jpg", StorageFileNameUtil.extensionOf("a.JPG"));
        assertEquals("c", StorageFileNameUtil.extensionOf("a.b.c"));
    }

    @Test
    void missingExtensionReturnsEmpty() {
        // 无扩展名 / null / 空串 / 以点结尾 → 空串
        assertEquals("", StorageFileNameUtil.extensionOf("noext"));
        assertEquals("", StorageFileNameUtil.extensionOf(null));
        assertEquals("", StorageFileNameUtil.extensionOf(""));
        assertEquals("", StorageFileNameUtil.extensionOf("a."));
    }
}