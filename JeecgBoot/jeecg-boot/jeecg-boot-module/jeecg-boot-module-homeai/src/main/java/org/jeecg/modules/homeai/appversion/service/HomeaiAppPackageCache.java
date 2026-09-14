package org.jeecg.modules.homeai.appversion.service;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.appversion.entity.HomeaiAppVersion;
import org.jeecg.modules.homeai.appversion.util.HomeaiAppPackageCacheId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 当前发布 APK / H5 zip 的服务器本地副本：指纹未变则直接读磁盘，避免每次更新请求都从 OSS 拉整包。
 */
@Slf4j
@Service
public class HomeaiAppPackageCache {

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    @Value("${jeecg.path.upload:./upload}")
    private String uploadPath;

    public Path dir() throws IOException {
        Path dir = Paths.get(uploadPath, "homeai", "app-package-cache");
        Files.createDirectories(dir);
        return dir;
    }

    public Path targetApk(HomeaiAppVersion row) throws IOException {
        return target(row == null ? null : row.getApkSha256(), row == null ? null : row.getApkUrl(), "apk");
    }

    //update-begin---author:cursor---date:2026-09-04---for:【APP热更新】zip 与 APK 一样走本地缓存，避免每次打 OSS---
    public Path targetResource(HomeaiAppVersion row) throws IOException {
        return target(row == null ? null : row.getResourceSha256(), row == null ? null : row.getResourceUrl(), "zip");
    }

    private Path target(String sha256, String storedReference, String extension) throws IOException {
        String id = HomeaiAppPackageCacheId.of(sha256, storedReference);
        return dir().resolve(HomeaiAppPackageCacheId.fileName(id, extension));
    }
    //update-end---author:cursor---date:2026-09-04---for:【APP热更新】zip 与 APK 一样走本地缓存---

    /** 本地已有完整安装包则返回路径，否则 null（需从 OSS 拉取）。 */
    public Path lookupApk(HomeaiAppVersion row) {
        try {
            Path dest = targetApk(row);
            if (Files.isRegularFile(dest) && Files.size(dest) > 0) {
                return dest;
            }
        } catch (Exception e) {
            log.debug("安装包本地缓存未命中: {}", e.getMessage());
        }
        return null;
    }

    public Path lookupResource(HomeaiAppVersion row) {
        try {
            Path dest = targetResource(row);
            if (Files.isRegularFile(dest) && Files.size(dest) > 0) {
                return dest;
            }
        } catch (Exception e) {
            log.debug("热更新包本地缓存未命中: {}", e.getMessage());
        }
        return null;
    }

    /** 管理端刚上传的文件立刻写入缓存，后续下载不必再打 OSS。 */
    public void rememberApk(Path source, String sha256, String storedReference) {
        remember(source, sha256, storedReference, "apk");
    }

    public void rememberZip(Path source, String sha256, String storedReference) {
        remember(source, sha256, storedReference, "zip");
    }

    private void remember(Path source, String sha256, String storedReference, String extension) {
        if (source == null || !Files.isRegularFile(source)) {
            return;
        }
        try {
            String id = HomeaiAppPackageCacheId.of(sha256, storedReference);
            Path dest = dir().resolve(HomeaiAppPackageCacheId.fileName(id, extension));
            Object lock = locks.computeIfAbsent(dest.toString(), k -> new Object());
            synchronized (lock) {
                Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                pruneOthers(dest);
            }
            log.info("更新包已写入本地缓存: {}", dest.getFileName());
        } catch (Exception e) {
            log.warn("更新包写入本地缓存失败（不影响上传）", e);
        }
    }

    public Object lockFor(Path dest) {
        return locks.computeIfAbsent(dest.toString(), k -> new Object());
    }

    public void pruneOthers(Path keep) {
        if (keep == null) {
            return;
        }
        Path parent = keep.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            return;
        }
        String keepName = keep.getFileName().toString();
        int dot = keepName.lastIndexOf('.');
        String ext = dot >= 0 ? keepName.substring(dot) : "";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent, "pkg-*")) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (p.equals(keep) || name.endsWith(".part")) {
                    continue;
                }
                if (!ext.isEmpty() && !name.endsWith(ext)) {
                    continue;
                }
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // ignore
                }
            }
        } catch (IOException e) {
            log.debug("清理旧安装包缓存失败: {}", e.getMessage());
        }
    }

    public static boolean isUsable(Path path) {
        try {
            return path != null && Files.isRegularFile(path) && Files.size(path) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean blankRef(String apkUrl) {
        return oConvertUtils.isEmpty(apkUrl);
    }
}
