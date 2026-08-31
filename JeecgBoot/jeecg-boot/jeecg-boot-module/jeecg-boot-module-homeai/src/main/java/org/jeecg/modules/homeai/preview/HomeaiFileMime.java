package org.jeecg.modules.homeai.preview;

import org.jeecg.common.util.oConvertUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * APP / 浏览器下载原文件时的 MIME 与 Content-Disposition。
 */
public final class HomeaiFileMime {

    private static final Map<String, String> MIME = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("txt", "text/plain;charset=UTF-8"),
            Map.entry("md", "text/markdown;charset=UTF-8"),
            Map.entry("csv", "text/csv;charset=UTF-8"),
            Map.entry("json", "application/json;charset=UTF-8"),
            Map.entry("log", "text/plain;charset=UTF-8"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("m4a", "audio/mp4"),
            Map.entry("zip", "application/zip")
    );

    private HomeaiFileMime() {
    }

    public static String mimeOf(String extension) {
        String ext = HomeaiPreviewKind.normalizeExt(extension);
        if (ext.isEmpty()) {
            return "application/octet-stream";
        }
        return MIME.getOrDefault(ext, "application/octet-stream");
    }

    public static String contentDisposition(String downloadName, String extension) {
        String name = oConvertUtils.isEmpty(downloadName) ? "file" : downloadName.trim();
        String ext = HomeaiPreviewKind.normalizeExt(extension);
        if (!ext.isEmpty() && !name.toLowerCase(Locale.ROOT).endsWith("." + ext)) {
            name = name + "." + ext;
        }
        String ascii = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (ascii.replace("_", "").replace(".", "").isEmpty()) {
            ascii = ext.isEmpty() ? "file.bin" : "file." + ext;
        }
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }

    public static void writeLocalFile(jakarta.servlet.http.HttpServletResponse response,
                                     Path path, String downloadName, String extension) throws IOException {
        if (path == null || !Files.isRegularFile(path)) {
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }
        response.setContentType(mimeOf(extension));
        response.setHeader("Content-Disposition", contentDisposition(downloadName, extension));
        response.setContentLengthLong(Files.size(path));
        try (InputStream in = Files.newInputStream(path); OutputStream out = response.getOutputStream()) {
            in.transferTo(out);
            out.flush();
        }
    }
}
