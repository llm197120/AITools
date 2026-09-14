package org.jeecg.modules.homeai.storage.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeaiGotenbergConvertTest {

    @Test
    void onlyPdfGoesToGotenberg() {
        assertTrue(HomeaiGotenbergConvert.handlesTarget("pdf"));
        assertFalse(HomeaiGotenbergConvert.handlesTarget("txt"));
        assertFalse(HomeaiGotenbergConvert.enabled(""));
        assertTrue(HomeaiGotenbergConvert.enabled("http://127.0.0.1:3000/"));
        assertFalse(HomeaiGotenbergConvert.enabled("https://gotenberg.example.com"));
        assertEquals(
                "http://127.0.0.1:3000/forms/libreoffice/convert",
                HomeaiGotenbergConvert.convertUri("http://127.0.0.1:3000/").toString());
    }
}
