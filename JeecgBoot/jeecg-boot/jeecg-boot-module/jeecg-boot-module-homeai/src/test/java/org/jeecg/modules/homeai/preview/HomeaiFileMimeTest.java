package org.jeecg.modules.homeai.preview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeaiFileMimeTest {

    @Test
    void mimeOfKnownOfficeAndPdf() {
        assertEquals("application/pdf", HomeaiFileMime.mimeOf("PDF"));
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                HomeaiFileMime.mimeOf("docx"));
        assertEquals("application/octet-stream", HomeaiFileMime.mimeOf(""));
        assertEquals("application/octet-stream", HomeaiFileMime.mimeOf("bin"));
    }

    @Test
    void dispositionKeepsExtensionAndUtf8Name() {
        String header = HomeaiFileMime.contentDisposition("会议纪要", "docx");
        assertTrue(header.contains("filename*=UTF-8''"));
        assertTrue(header.contains(".docx"));
        assertTrue(header.startsWith("attachment;"));
    }
}
