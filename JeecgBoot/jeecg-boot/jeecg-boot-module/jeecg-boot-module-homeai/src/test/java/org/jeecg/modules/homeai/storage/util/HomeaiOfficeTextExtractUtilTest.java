package org.jeecg.modules.homeai.storage.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeaiOfficeTextExtractUtilTest {

    @TempDir
    Path tmp;

    @Test
    void supportsDocxToTxt() {
        assertTrue(HomeaiOfficeTextExtractUtil.supports("docx", "txt"));
        assertTrue(HomeaiOfficeTextExtractUtil.supports(".DOCX", "TXT"));
        assertFalse(HomeaiOfficeTextExtractUtil.supports("docx", "pdf"));
    }

    @Test
    void wordXmlToTextKeepsParagraphs() {
        String xml = "<w:document><w:body>"
                + "<w:p><w:r><w:t>家庭资料转换</w:t></w:r></w:p>"
                + "<w:p><w:r><w:t xml:space=\"preserve\">第二段</w:t></w:r></w:p>"
                + "</w:body></w:document>";
        String body = HomeaiOfficeTextExtractUtil.wordXmlToText(xml);
        assertTrue(body.contains("家庭资料转换"));
        assertTrue(body.contains("第二段"));
    }

    @Test
    void extractDocxZipToUtf8Txt() throws Exception {
        Path docx = tmp.resolve("sample.docx");
        writeMinimalDocx(docx, "家庭资料转换");
        Path txt = HomeaiOfficeTextExtractUtil.extract(docx, tmp.resolve("out"), "txt");
        assertTrue(Files.exists(txt));
        assertTrue(Files.readString(txt, StandardCharsets.UTF_8).contains("家庭资料转换"));
    }

    /** 手写最小 OOXML，避开 POI write 对较新 commons-io 的依赖 */
    private static void writeMinimalDocx(Path dest, String text) throws Exception {
        String documentXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                + "<w:body><w:p><w:r><w:t>" + text + "</w:t></w:r></w:p></w:body></w:document>";
        String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/word/document.xml\" "
                + "ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                + "</Types>";
        String rels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" "
                + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" "
                + "Target=\"word/document.xml\"/>"
                + "</Relationships>";
        try (OutputStream out = Files.newOutputStream(dest);
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            put(zip, "[Content_Types].xml", contentTypes);
            put(zip, "_rels/.rels", rels);
            put(zip, "word/document.xml", documentXml);
        }
    }

    private static void put(ZipOutputStream zip, String name, String xml) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(xml.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
