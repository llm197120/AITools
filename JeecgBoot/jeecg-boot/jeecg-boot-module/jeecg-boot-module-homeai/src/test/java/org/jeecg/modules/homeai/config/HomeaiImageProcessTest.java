package org.jeecg.modules.homeai.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeaiImageProcessTest {

    @Test
    void processableRasterImages() {
        assertTrue(HomeaiImageProcess.isProcessableImage("cover.JPG"));
        assertTrue(HomeaiImageProcess.isProcessableImage("oss:homeai/a/b.webp"));
        assertTrue(HomeaiImageProcess.isProcessableImage("https://x/y/z.png?Expires=1"));
        assertFalse(HomeaiImageProcess.isProcessableImage("anim.gif"));
        assertFalse(HomeaiImageProcess.isProcessableImage("notes.docx"));
        assertFalse(HomeaiImageProcess.isProcessableImage(""));
        assertTrue(HomeaiImageProcess.isProcessableImage("png"));
    }

    @Test
    void extensionIgnoresQuery() {
        assertEquals("png", HomeaiImageProcess.extensionOf("https://bucket.oss/a.PNG?x=1"));
        assertEquals("jpg", HomeaiImageProcess.extensionOf("oss:homeai/u/1.jpg"));
    }
}
