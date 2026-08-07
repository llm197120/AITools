package org.jeecg.modules.homeai.storage.util;

import org.jeecg.common.util.oConvertUtils;

/**
 * 资料存储文件名工具：展示名（originalName）与 OSS/磁盘存储名（storedName）分离
 */
public final class StorageFileNameUtil {

    private static final int MAX_ORIGINAL_NAME_LEN = 300;

    private StorageFileNameUtil() {
    }

    /** OSS/本地对象路径：homeai/{userId}/{storedName}，禁止使用 originalName */
    public static String buildObjectKey(String userId, String storedName) {
        if (oConvertUtils.isEmpty(userId) || oConvertUtils.isEmpty(storedName)) {
            throw new IllegalArgumentException("userId 与 storedName 不能为空");
        }
        return "homeai/" + userId + "/" + storedName;
    }

    /** 清洗并截断用户可见的原始文件名 */
    public static String sanitizeOriginalName(String name) {
        if (oConvertUtils.isEmpty(name)) {
            return "unknown";
        }
        String trimmed = name.trim().replace("\\", "/");
        int slash = trimmed.lastIndexOf('/');
        if (slash >= 0) {
            trimmed = trimmed.substring(slash + 1);
        }
        trimmed = trimmed.replaceAll("[\\x00-\\x1f]", "").replaceAll("[/\\\\:*?\"<>|]", "_");
        if (trimmed.length() > MAX_ORIGINAL_NAME_LEN) {
            int dot = trimmed.lastIndexOf('.');
            if (dot > 0 && dot < trimmed.length() - 1) {
                String ext = trimmed.substring(dot);
                int keep = MAX_ORIGINAL_NAME_LEN - ext.length();
                trimmed = (keep > 0 ? trimmed.substring(0, keep) : "file") + ext;
            } else {
                trimmed = trimmed.substring(0, MAX_ORIGINAL_NAME_LEN);
            }
        }
        return trimmed.isEmpty() ? "unknown" : trimmed;
    }

    /** 微信临时路径/无意义 multipart 原名 */
    public static boolean isTempUploadName(String name) {
        if (oConvertUtils.isEmpty(name)) {
            return true;
        }
        String base = sanitizeOriginalName(name).toLowerCase();
        return base.startsWith("tmp")
                || base.startsWith("temp")
                || base.contains("wxfile")
                || base.matches("^\\d+\\.(jpg|jpeg|png|gif|webp|bmp|mp4|mov)$");
    }

    public static String extensionOf(String fileName) {
        if (oConvertUtils.isEmpty(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
