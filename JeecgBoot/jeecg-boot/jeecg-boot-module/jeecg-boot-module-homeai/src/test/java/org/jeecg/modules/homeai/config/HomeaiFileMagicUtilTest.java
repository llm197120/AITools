package org.jeecg.modules.homeai.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件头魔数校验工具纯逻辑测试
 *
 * <p>覆盖 HomeaiFileMagicUtil.matchesMagic 各扩展名魔数判定，以及
 * validate 的扩展名白名单（SKIP_MAGIC）、可执行文件拦截、内容与扩展名匹配校验。
 * 无 Spring 上下文、无 Mockito，仅 JUnit 5 + MockMultipartFile。</p>
 */
class HomeaiFileMagicUtilTest {

    // ---------- matchesMagic：各扩展名魔数判定 ----------

    @Test
    void jpgMagicPasses() {
        // JPEG 魔数 FF D8 FF
        byte[] header = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        assertTrue(HomeaiFileMagicUtil.matchesMagic(header, "jpg"));
        assertTrue(HomeaiFileMagicUtil.matchesMagic(header, "jpeg"));
    }

    @Test
    void jpgWrongMagicRejected() {
        // 全零头不匹配 JPEG 魔数
        byte[] header = {0x00, 0x00, 0x00, 0x00};
        assertFalse(HomeaiFileMagicUtil.matchesMagic(header, "jpg"));
    }

    @Test
    void pngMagicPasses() {
        // PNG 魔数 89 50 4E 47
        byte[] header = {(byte) 0x89, 'P', 'N', 'G', 0x0D};
        assertTrue(HomeaiFileMagicUtil.matchesMagic(header, "png"));
    }

    @Test
    void gifMagicPasses() {
        // GIF 魔数 GIF8
        byte[] header = {'G', 'I', 'F', '8', '9', 'a'};
        assertTrue(HomeaiFileMagicUtil.matchesMagic(header, "gif"));
    }

    @Test
    void pdfMagicPasses() {
        // PDF 魔数 %PDF
        byte[] header = {'%', 'P', 'D', 'F', '-', '1', '.', '4'};
        assertTrue(HomeaiFileMagicUtil.matchesMagic(header, "pdf"));
    }

    @Test
    void zipFamilyMagicPasses() {
        // ZIP 家族（zip/docx/xlsx/pptx）魔数 PK
        byte[] header = {'P', 'K', 0x03, 0x04};
        assertTrue(HomeaiFileMagicUtil.matchesMagic(header, "zip"));
        assertTrue(HomeaiFileMagicUtil.matchesMagic(header, "docx"));
        assertTrue(HomeaiFileMagicUtil.matchesMagic(header, "xlsx"));
        assertTrue(HomeaiFileMagicUtil.matchesMagic(header, "pptx"));
    }

    @Test
    void oleMagicPasses() {
        // OLE2（doc/xls/ppt）魔数 D0 CF 11 E0
        byte[] header = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1};
        assertTrue(HomeaiFileMagicUtil.matchesMagic(header, "doc"));
        assertTrue(HomeaiFileMagicUtil.matchesMagic(header, "xls"));
        assertTrue(HomeaiFileMagicUtil.matchesMagic(header, "ppt"));
    }

    @Test
    void mp4MagicPasses() {
        // MP4 头含 ASCII "ftyp"
        byte[] header = {0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p'};
        assertTrue(HomeaiFileMagicUtil.matchesMagic(header, "mp4"));
    }

    @Test
    void unknownExtensionDefaultsToTrue() {
        // 未登记扩展名走 default 分支，直接放行
        byte[] header = {0x01, 0x02, 0x03};
        assertTrue(HomeaiFileMagicUtil.matchesMagic(header, "webp"));
    }

    @Test
    void nullOrTooShortHeaderRejected() {
        // null 头或长度不足 2 一律拒绝
        assertFalse(HomeaiFileMagicUtil.matchesMagic(null, "jpg"));
        assertFalse(HomeaiFileMagicUtil.matchesMagic(new byte[]{0x01}, "jpg"));
    }

    // ---------- validate：上传校验 ----------

    @Test
    void nullFileThrows() {
        // file 为 null → 无法校验文件类型
        IOException ex = assertThrows(IOException.class,
                () -> HomeaiFileMagicUtil.validate(null, "jpg"));
        assertTrue(ex.getMessage().contains("无法校验文件类型"));
    }

    @Test
    void emptyExtensionThrows() {
        // 扩展名为空（null/空串）→ 无法校验文件类型
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        assertThrows(IOException.class, () -> HomeaiFileMagicUtil.validate(file, ""));
        assertThrows(IOException.class, () -> HomeaiFileMagicUtil.validate(file, null));
    }

    @Test
    void executableContentInSkippedExtensionRejected() {
        // txt 属 SKIP_MAGIC 扩展名，但内容为 MZ 可执行文件头 → 拒绝
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain",
                new byte[]{'M', 'Z', 0x00, 0x00});
        IOException ex = assertThrows(IOException.class,
                () -> HomeaiFileMagicUtil.validate(file, "txt"));
        assertTrue(ex.getMessage().contains("不允许上传可执行文件"));
    }

    @Test
    void plainTextInSkippedExtensionPasses() {
        // txt 普通文本内容 → 不抛异常
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain",
                "hello world".getBytes());
        assertDoesNotThrow(() -> HomeaiFileMagicUtil.validate(file, "txt"));
    }

    @Test
    void jpgContentMismatchRejected() {
        // jpg 扩展名但内容不是 JPEG 魔数 → 内容与扩展名不匹配
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg",
                "not a jpg".getBytes());
        IOException ex = assertThrows(IOException.class,
                () -> HomeaiFileMagicUtil.validate(file, "jpg"));
        assertTrue(ex.getMessage().contains("文件内容与扩展名不匹配"));
    }

    @Test
    void jpgValidMagicPasses() {
        // jpg 扩展名 + 合法 JPEG 魔数 → 不抛异常
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10});
        assertDoesNotThrow(() -> HomeaiFileMagicUtil.validate(file, "jpg"));
    }
}