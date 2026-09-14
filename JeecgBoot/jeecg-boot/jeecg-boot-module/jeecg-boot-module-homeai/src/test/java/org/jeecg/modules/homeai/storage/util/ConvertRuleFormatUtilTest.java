package org.jeecg.modules.homeai.storage.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvertRuleFormatUtilTest {

    @Test
    void normalizeStripsDotAndLowercases() {
        assertEquals("docx", ConvertRuleFormatUtil.normalize(" .DOCX "));
        assertEquals("pdf", ConvertRuleFormatUtil.normalize("PDF"));
        assertEquals("", ConvertRuleFormatUtil.normalize(null));
    }

    @Test
    void samePairIgnoresCase() {
        assertTrue(ConvertRuleFormatUtil.samePair("DOCX", "PDF", "docx", "pdf"));
        assertFalse(ConvertRuleFormatUtil.samePair("docx", "txt", "docx", "pdf"));
    }
}
