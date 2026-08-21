package org.jeecg.modules.homeai.preview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeaiPreviewKindTest {

    @Test
    void ofExtensionMapsCommonTypes() {
        assertEquals(HomeaiPreviewKind.IMAGE, HomeaiPreviewKind.ofExtension("webp"));
        assertEquals(HomeaiPreviewKind.VIDEO, HomeaiPreviewKind.ofExtension("mp4"));
        assertEquals(HomeaiPreviewKind.AUDIO, HomeaiPreviewKind.ofExtension("mp3"));
        assertEquals(HomeaiPreviewKind.PDF, HomeaiPreviewKind.ofExtension("pdf"));
        assertEquals(HomeaiPreviewKind.OFFICE, HomeaiPreviewKind.ofExtension("docx"));
        assertEquals(HomeaiPreviewKind.TEXT, HomeaiPreviewKind.ofExtension("md"));
        assertEquals(HomeaiPreviewKind.ARCHIVE, HomeaiPreviewKind.ofExtension("zip"));
        assertEquals(HomeaiPreviewKind.ARCHIVE, HomeaiPreviewKind.ofExtension("apk"));
    }

    @Test
    void ofLearnTypePrefersMaterialType() {
        assertEquals(HomeaiPreviewKind.LINK, HomeaiPreviewKind.ofLearnType("link", "html"));
        assertEquals(HomeaiPreviewKind.AUDIO, HomeaiPreviewKind.ofLearnType("audio", "mp3"));
        assertEquals(HomeaiPreviewKind.OFFICE, HomeaiPreviewKind.ofLearnType("xls", "xlsx"));
        assertEquals(HomeaiPreviewKind.TEXT, HomeaiPreviewKind.ofLearnType("note", "txt"));
    }

    @Test
    void uploadCategoryCollapsesPdfOfficeToDocument() {
        assertEquals("document", HomeaiPreviewKind.uploadCategory("pdf"));
        assertEquals("document", HomeaiPreviewKind.uploadCategory("docx"));
        assertEquals(HomeaiPreviewKind.VIDEO, HomeaiPreviewKind.uploadCategory("mp4"));
    }
}
