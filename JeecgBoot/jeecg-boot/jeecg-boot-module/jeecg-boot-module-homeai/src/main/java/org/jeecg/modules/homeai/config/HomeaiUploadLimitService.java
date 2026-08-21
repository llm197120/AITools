package org.jeecg.modules.homeai.config;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.preview.HomeaiPreviewKind;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 单文件分类大小上限（独立于空间配额与 Spring multipart）
 */
@Component
public class HomeaiUploadLimitService {

    @Value("${homeai.upload.limits.video:209715200}")
    private long videoLimit;

    @Value("${homeai.upload.limits.audio:52428800}")
    private long audioLimit;

    @Value("${homeai.upload.limits.image:20971520}")
    private long imageLimit;

    @Value("${homeai.upload.limits.document:52428800}")
    private long documentLimit;

    @Value("${homeai.upload.limits.archive:104857600}")
    private long archiveLimit;

    @Value("${homeai.upload.limits.text:10485760}")
    private long textLimit;

    //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R63】按扩展名拦截单文件大小-----------
    public void assertAllowed(String extension, long fileSize) {
        if (fileSize <= 0) {
            return;
        }
        String category = HomeaiPreviewKind.uploadCategory(extension);
        long limit = limitOf(category);
        if (fileSize > limit) {
            throw new JeecgBootException("文件过大：当前 "
                    + formatBytes(fileSize) + "，" + categoryLabel(category)
                    + "上限 " + formatBytes(limit));
        }
    }

    public long limitOf(String category) {
        if (oConvertUtils.isEmpty(category)) {
            return documentLimit;
        }
        switch (category) {
            case HomeaiPreviewKind.VIDEO:
                return videoLimit;
            case HomeaiPreviewKind.AUDIO:
                return audioLimit;
            case HomeaiPreviewKind.IMAGE:
                return imageLimit;
            case HomeaiPreviewKind.ARCHIVE:
                return archiveLimit;
            case HomeaiPreviewKind.TEXT:
                return textLimit;
            default:
                return documentLimit;
        }
    }

    private static String categoryLabel(String category) {
        if (HomeaiPreviewKind.VIDEO.equals(category)) return "视频";
        if (HomeaiPreviewKind.AUDIO.equals(category)) return "音频";
        if (HomeaiPreviewKind.IMAGE.equals(category)) return "图片";
        if (HomeaiPreviewKind.ARCHIVE.equals(category)) return "压缩包";
        if (HomeaiPreviewKind.TEXT.equals(category)) return "文本";
        return "文档";
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1fMB", bytes / (1024.0 * 1024));
        return String.format("%.2fGB", bytes / (1024.0 * 1024 * 1024));
    }
    //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R63】按扩展名拦截单文件大小-----------
}
