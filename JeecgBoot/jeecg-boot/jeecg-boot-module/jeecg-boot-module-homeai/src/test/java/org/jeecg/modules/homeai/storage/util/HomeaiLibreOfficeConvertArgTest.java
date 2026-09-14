package org.jeecg.modules.homeai.storage.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeaiLibreOfficeConvertArgTest {

    @Test
    void txtUsesTextFilter() {
        assertEquals("txt:Text", HomeaiLibreOfficeConvertArg.of("txt"));
        assertEquals("txt:Text", HomeaiLibreOfficeConvertArg.of(".TXT"));
    }

    @Test
    void otherFormatsStayBare() {
        assertEquals("pdf", HomeaiLibreOfficeConvertArg.of("pdf"));
        assertEquals("docx", HomeaiLibreOfficeConvertArg.of("DOCX"));
        assertEquals("pdf", HomeaiLibreOfficeConvertArg.of(null));
    }
}
