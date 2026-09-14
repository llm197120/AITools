package org.jeecg.modules.homeai.storage.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 不依赖本机 Office / POI：从 docx（OOXML zip）抽出纯文本。
 */
public final class HomeaiOfficeTextExtractUtil {

    private static final Pattern PARAGRAPH = Pattern.compile("<w:p[\\s>]");
    private static final Pattern TEXT_RUN = Pattern.compile("<w:t(?:\\s[^>]*)?>(.*?)</w:t>", Pattern.DOTALL);

    private HomeaiOfficeTextExtractUtil() {
    }

    public static boolean supports(String sourceExt, String targetFormat) {
        String src = ConvertRuleFormatUtil.normalize(sourceExt);
        String dst = ConvertRuleFormatUtil.normalize(targetFormat);
        return "txt".equals(dst) && ("docx".equals(src) || "txt".equals(src));
    }

    public static Path extract(Path sourcePath, Path outDir, String targetFormat) throws IOException {
        String srcExt = ConvertRuleFormatUtil.normalize(
                StorageFileNameUtil.extensionOf(sourcePath.getFileName().toString()));
        String dst = ConvertRuleFormatUtil.normalize(targetFormat);
        if (!supports(srcExt, dst)) {
            return null;
        }
        Files.createDirectories(outDir);
        String base = fileNameWithoutExt(sourcePath.getFileName().toString());
        Path dest = outDir.resolve(base + "." + dst);
        if ("txt".equals(srcExt)) {
            Files.copy(sourcePath, dest, StandardCopyOption.REPLACE_EXISTING);
            return dest;
        }
        String text = extractDocx(sourcePath);
        Files.writeString(dest, text == null ? "" : text, StandardCharsets.UTF_8);
        return dest;
    }

    static String wordXmlToText(String xml) {
        if (xml == null || xml.isBlank()) {
            return "";
        }
        String[] parts = PARAGRAPH.split(xml);
        StringBuilder out = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            Matcher m = TEXT_RUN.matcher(parts[i]);
            StringBuilder line = new StringBuilder();
            while (m.find()) {
                line.append(unescapeXml(m.group(1)));
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(line);
        }
        return out.toString().trim();
    }

    private static String extractDocx(Path sourcePath) throws IOException {
        try (InputStream in = Files.newInputStream(sourcePath);
             ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    return wordXmlToText(xml);
                }
            }
        }
        throw new IOException("docx 缺少 word/document.xml");
    }

    private static String unescapeXml(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }

    private static String fileNameWithoutExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
