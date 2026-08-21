package org.jeecg.modules.homeai.preview;

import org.jeecg.common.util.oConvertUtils;

import java.util.Locale;
import java.util.Set;

/**
 * 预览 kind 与上传分类（扩展名 → image/video/audio/pdf/office/text/archive/link）
 */
public final class HomeaiPreviewKind {

    public static final String IMAGE = "image";
    public static final String VIDEO = "video";
    public static final String AUDIO = "audio";
    public static final String PDF = "pdf";
    public static final String OFFICE = "office";
    public static final String TEXT = "text";
    public static final String ARCHIVE = "archive";
    public static final String LINK = "link";
    public static final String UNKNOWN = "unknown";

    private static final Set<String> IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final Set<String> VIDEO_EXTS = Set.of("mp4", "mov", "avi", "mkv", "webm", "m4v");
    private static final Set<String> AUDIO_EXTS = Set.of("mp3", "wav", "m4a", "aac");
    private static final Set<String> OFFICE_EXTS = Set.of("doc", "docx", "xls", "xlsx", "ppt", "pptx");
    private static final Set<String> TEXT_EXTS = Set.of("txt", "md", "csv", "log", "json");
    //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R69】apk 按压缩包分类-----------
    private static final Set<String> ARCHIVE_EXTS = Set.of("zip", "rar", "7z", "apk");
    //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R69】apk 按压缩包分类-----------

    private HomeaiPreviewKind() {
    }

    public static String ofExtension(String extension) {
        String ext = normalizeExt(extension);
        if (ext.isEmpty()) {
            return UNKNOWN;
        }
        if (IMAGE_EXTS.contains(ext)) return IMAGE;
        if (VIDEO_EXTS.contains(ext)) return VIDEO;
        if (AUDIO_EXTS.contains(ext)) return AUDIO;
        if ("pdf".equals(ext)) return PDF;
        if (OFFICE_EXTS.contains(ext)) return OFFICE;
        if (TEXT_EXTS.contains(ext)) return TEXT;
        if (ARCHIVE_EXTS.contains(ext)) return ARCHIVE;
        return UNKNOWN;
    }

    /** 学习资料优先用 type，扩展名作兜底 */
    public static String ofLearnType(String type, String extension) {
        String t = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (LINK.equals(t)) {
            return LINK;
        }
        if (AUDIO.equals(t)) {
            return AUDIO;
        }
        if ("doc".equals(t) || "xls".equals(t) || "ppt".equals(t)) {
            return OFFICE;
        }
        if ("note".equals(t)) {
            return TEXT;
        }
        if (IMAGE.equals(t) || VIDEO.equals(t) || PDF.equals(t)) {
            return t;
        }
        return ofExtension(extension);
    }

    /** 上传大小分类：pdf/office 都走 document */
    public static String uploadCategory(String extension) {
        String kind = ofExtension(extension);
        if (PDF.equals(kind) || OFFICE.equals(kind) || UNKNOWN.equals(kind)) {
            return "document";
        }
        return kind;
    }

    public static boolean isOffice(String kind) {
        return OFFICE.equals(kind);
    }

    public static String normalizeExt(String extension) {
        if (oConvertUtils.isEmpty(extension)) {
            return "";
        }
        String ext = extension.trim().toLowerCase(Locale.ROOT);
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        int q = ext.indexOf('?');
        if (q >= 0) {
            ext = ext.substring(0, q);
        }
        return ext;
    }

    public static String extensionOfNameOrUrl(String nameOrUrl) {
        if (oConvertUtils.isEmpty(nameOrUrl)) {
            return "";
        }
        String path = nameOrUrl.split("\\?")[0];
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? normalizeExt(name.substring(dot + 1)) : "";
    }
}
