package org.jeecg.modules.homeai.appversion.util;

import org.jeecg.common.util.oConvertUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * 安装包本地缓存文件名：优先用内容 SHA-256；没有则用存储引用摘要。
 * 同一指纹视为「安装包未更新」，命中后不再从 OSS 拉取。
 */
public final class HomeaiAppPackageCacheId {

    private HomeaiAppPackageCacheId() {
    }

    public static String of(String sha256, String storedReference) {
        String sha = oConvertUtils.isEmpty(sha256) ? "" : sha256.trim().toLowerCase(Locale.ROOT);
        if (sha.matches("[0-9a-f]{32,}")) {
            return sha;
        }
        String ref = oConvertUtils.isEmpty(storedReference) ? "" : storedReference.trim();
        return "ref-" + sha256Hex(ref.getBytes(StandardCharsets.UTF_8));
    }

    public static String fileName(String cacheId, String extension) {
        String ext = oConvertUtils.isEmpty(extension) ? "bin" : extension.replaceAll("[^A-Za-z0-9]", "");
        if (ext.isEmpty()) {
            ext = "bin";
        }
        return "pkg-" + cacheId + "." + ext;
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
