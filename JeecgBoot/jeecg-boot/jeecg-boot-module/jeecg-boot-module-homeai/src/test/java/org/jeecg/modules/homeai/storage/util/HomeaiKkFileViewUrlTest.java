package org.jeecg.modules.homeai.storage.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeaiKkFileViewUrlTest {

    @Test
    void emptyWhenNotConfigured() {
        assertEquals("", HomeaiKkFileViewUrl.build("", "http://a/b.docx", "a.docx"));
        assertEquals("", HomeaiKkFileViewUrl.build("http://127.0.0.1:8012", "", "a.docx"));
    }

    @Test
    void encodesFileUrl() {
        String url = HomeaiKkFileViewUrl.build("http://127.0.0.1:8012/", "http://host/f.docx", "报告.docx");
        assertTrue(url.startsWith("http://127.0.0.1:8012/onlinePreview?url="));
        assertTrue(url.contains("url="));
    }

    @Test
    void ignoresPublicKkFileView() {
        assertEquals("", HomeaiKkFileViewUrl.build("https://file.kkview.cn", "http://a/b.docx", "a.docx"));
    }
}
