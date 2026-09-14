package org.jeecg.modules.homeai.config.util;

import org.jeecg.common.exception.JeecgBootException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeaiDocHtmlUtilTest {

    @Test
    void stripsScriptAndKeepsParagraph() {
        String out = HomeaiDocHtmlUtil.sanitize("<p>协议</p><script>alert(1)</script>");
        assertTrue(out.contains("协议"));
        assertFalse(out.toLowerCase().contains("script"));
        assertFalse(out.contains("alert"));
    }

    @Test
    void rejectsTooLong() {
        String huge = "a".repeat(HomeaiDocHtmlUtil.MAX_CHARS + 1);
        assertThrows(JeecgBootException.class, () -> HomeaiDocHtmlUtil.sanitize(huge));
    }
}
