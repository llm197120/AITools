package org.jeecg.modules.homeai.config.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.common.util.oss.OssBootUtil;
import org.jeecg.modules.homeai.config.HomeaiFileUrlUtil;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Slf4j
@Service
public class HomeaiFileStorageServiceImpl implements IHomeaiFileStorageService {

    @Value("${jeecg.path.upload:./upload}")
    private String uploadPath;

    @Value("${jeecg.uploadType:local}")
    private String uploadType;

    @Value("${homeai.oss.private-bucket:true}")
    private boolean privateOssBucket;

    @Value("${homeai.oss.presign-expire-seconds:7200}")
    private long presignExpireSeconds;

    @Override
    public boolean isOssEnabled() {
        return CommonConstant.UPLOAD_TYPE_OSS.equals(uploadType);
    }

    @Override
    public boolean isPrivateOssBucket() {
        return isOssEnabled() && privateOssBucket;
    }

    @Override
    public String storeMultipart(MultipartFile file, String objectKey) {
        String key = normalizeObjectKey(objectKey);
        if (isOssEnabled()) {
            return storeToOss(file::getInputStream, file.getSize(), file.getContentType(), key);
        }
        try {
            Path target = localPath(key);
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());
            return HomeaiFileUrlUtil.toAbsoluteUrl("/upload/" + key);
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败", e);
        }
    }

    @Override
    public String storeLocalFile(Path localFile, String objectKey) {
        String key = normalizeObjectKey(objectKey);
        if (isOssEnabled()) {
            try {
                return storeToOss(() -> Files.newInputStream(localFile), Files.size(localFile), null, key);
            } catch (IOException e) {
                throw new RuntimeException("OSS 上传失败", e);
            }
        }
        Path target = localPath(key);
        if (!localFile.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) {
            try {
                Files.createDirectories(target.getParent());
                Files.copy(localFile, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("文件保存失败", e);
            }
        }
        return HomeaiFileUrlUtil.toAbsoluteUrl("/upload/" + key);
    }

    @Override
    public String normalizeStoredReference(String url) {
        if (oConvertUtils.isEmpty(url)) {
            return url;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith(OSS_REF_PREFIX) || trimmed.startsWith("data:")) {
            return trimmed;
        }
        if (isOssEnabled()) {
            try {
                return toOssReference(extractObjectKey(trimmed));
            } catch (Exception e) {
                log.debug("无法规范化为 OSS 引用，保留原值: {}", trimmed);
            }
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("/upload")) {
            return HomeaiFileUrlUtil.toAbsoluteUrl(trimmed);
        }
        return trimmed;
    }

    @Override
    public String resolveAccessUrl(String storedReference) {
        if (oConvertUtils.isEmpty(storedReference) || storedReference.startsWith("data:")) {
            return storedReference;
        }
        if (!isOssEnabled()) {
            if (storedReference.startsWith("http://") || storedReference.startsWith("https://")) {
                return storedReference;
            }
            if (storedReference.startsWith("/upload")) {
                return HomeaiFileUrlUtil.toAbsoluteUrl(storedReference);
            }
            return HomeaiFileUrlUtil.toAbsoluteUrl("/upload/" + storedReference.replaceFirst("^/+", ""));
        }
        if (isPrivateOssBucket()) {
            String objectKey = extractObjectKey(storedReference);
            String signed = OssBootUtil.getPresignedUrl(objectKey, presignExpireSeconds);
            if (oConvertUtils.isEmpty(signed)) {
                log.warn("预签名 URL 生成失败: {}", storedReference);
                return storedReference;
            }
            return signed;
        }
        return toPublicOssUrl(extractObjectKey(storedReference));
    }

    @Override
    public void applyAccessUrl(StorageFile file) {
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R21】缩略图 URL 同步签名/解析-----------
        if (file == null) {
            return;
        }
        if (oConvertUtils.isNotEmpty(file.getFileUrl())) {
            file.setFileUrl(resolveAccessUrl(file.getFileUrl()));
        }
        if (oConvertUtils.isNotEmpty(file.getThumbnailUrl())) {
            file.setThumbnailUrl(resolveAccessUrl(file.getThumbnailUrl()));
        }
        //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R63】预览 PDF URL 同步签名-----------
        if (oConvertUtils.isNotEmpty(file.getPreviewPdfUrl())) {
            file.setPreviewPdfUrl(resolveAccessUrl(file.getPreviewPdfUrl()));
        }
        //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R63】预览 PDF URL 同步签名-----------
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R21】缩略图 URL 同步签名/解析-----------
    }

    @Override
    public void applyAccessUrls(List<StorageFile> files) {
        if (files == null) {
            return;
        }
        for (StorageFile file : files) {
            applyAccessUrl(file);
        }
    }

    @Override
    public Path resolveLocalPath(String storedReference) {
        if (oConvertUtils.isEmpty(storedReference)) {
            throw new IllegalArgumentException("fileUrl 为空");
        }
        if (isOssEnabled() && isOssStoredReference(storedReference)) {
            return downloadOssToTemp(extractObjectKey(storedReference));
        }
        if (isOssEnabled() && isRemoteUrl(storedReference)) {
            return downloadOssToTemp(extractObjectKey(storedReference));
        }
        String relative = HomeaiFileUrlUtil.toRelativeUrl(storedReference);
        if (oConvertUtils.isNotEmpty(relative) && relative.startsWith("/upload/")) {
            return Paths.get(uploadPath, relative.substring("/upload/".length()));
        }
        return Paths.get(uploadPath, storedReference);
    }

    @Override
    public void deleteIfExists(String storedReference) {
        if (oConvertUtils.isEmpty(storedReference)) {
            return;
        }
        if (isOssEnabled() && (isOssStoredReference(storedReference) || isRemoteUrl(storedReference))) {
            try {
                OssBootUtil.deleteObject(extractObjectKey(storedReference));
            } catch (Exception e) {
                log.warn("OSS 文件删除失败: {}", storedReference, e);
            }
            return;
        }
        try {
            Files.deleteIfExists(resolveLocalPath(storedReference));
        } catch (Exception e) {
            log.warn("本地文件删除失败: {}", storedReference, e);
        }
    }

    @Override
    public String extractObjectKey(String storedReference) {
        if (oConvertUtils.isEmpty(storedReference)) {
            throw new IllegalArgumentException("storedReference 为空");
        }
        String ref = storedReference.trim();
        if (ref.startsWith(OSS_REF_PREFIX)) {
            return ref.substring(OSS_REF_PREFIX.length());
        }
        int queryIdx = ref.indexOf('?');
        if (queryIdx > 0) {
            ref = ref.substring(0, queryIdx);
        }
        String staticDomain = OssBootUtil.getStaticDomain();
        if (oConvertUtils.isNotEmpty(staticDomain) && ref.startsWith(staticDomain)) {
            return ref.substring(staticDomain.length()).replaceFirst("^/+", "");
        }
        String endpoint = OssBootUtil.getEndPoint();
        String bucket = OssBootUtil.getBucketName();
        if (oConvertUtils.isNotEmpty(endpoint) && oConvertUtils.isNotEmpty(bucket)) {
            String httpsPrefix = "https://" + bucket + "." + endpoint + "/";
            if (ref.startsWith(httpsPrefix)) {
                return ref.substring(httpsPrefix.length());
            }
            String httpPrefix = "http://" + bucket + "." + endpoint + "/";
            if (ref.startsWith(httpPrefix)) {
                return ref.substring(httpPrefix.length());
            }
        }
        int idx = ref.indexOf("/homeai/");
        if (idx >= 0) {
            return ref.substring(idx + 1);
        }
        if (ref.startsWith("homeai/")) {
            return ref;
        }
        throw new IllegalArgumentException("无法解析 OSS 对象路径: " + storedReference);
    }

    private Path downloadOssToTemp(String objectKey) {
        InputStream in = OssBootUtil.getOssFile(objectKey, null);
        if (in == null) {
            throw new RuntimeException("无法从 OSS 读取文件: " + objectKey);
        }
        try (InputStream stream = in) {
            String suffix = objectKey.contains(".") ? objectKey.substring(objectKey.lastIndexOf('.')) : ".tmp";
            Path temp = Files.createTempFile("homeai_", suffix);
            Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp;
        } catch (IOException e) {
            throw new RuntimeException("下载 OSS 文件失败", e);
        }
    }

    private String toOssReference(String objectKey) {
        return OSS_REF_PREFIX + objectKey;
    }

    private String toPublicOssUrl(String objectKey) {
        String staticDomain = OssBootUtil.getStaticDomain();
        if (oConvertUtils.isNotEmpty(staticDomain)) {
            return staticDomain.replaceAll("/+$", "") + "/" + objectKey;
        }
        return "https://" + OssBootUtil.getBucketName() + "." + OssBootUtil.getEndPoint() + "/" + objectKey;
    }

    private boolean isOssStoredReference(String ref) {
        return ref.startsWith(OSS_REF_PREFIX);
    }

    private boolean isRemoteUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private Path localPath(String objectKey) {
        return Paths.get(uploadPath, objectKey.split("/"));
    }

    //update-begin---author:cursor---date:2026-08-21---for:【OSS】连接重置时重建客户端并重试，上传带文件大小---
    @FunctionalInterface
    private interface OssStreamSupplier {
        InputStream open() throws IOException;
    }

    private String storeToOss(OssStreamSupplier stream, long size, String contentType, String key) {
        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try (InputStream in = stream.open()) {
                String uploadedKey = OssBootUtil.uploadPrivate(in, key, size, contentType);
                if (oConvertUtils.isEmpty(uploadedKey)) {
                    throw new RuntimeException("OSS 上传失败");
                }
                return toOssReference(uploadedKey);
            } catch (Exception e) {
                last = e;
                log.warn("OSS 上传失败 attempt={}/{} key={}: {}", attempt, 3, key, e.getMessage());
                if (!OssBootUtil.isTransientNetworkFailure(e) || attempt == 3) {
                    break;
                }
                OssBootUtil.resetClient();
                try {
                    Thread.sleep(400L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new RuntimeException(last != null ? last.getMessage() : "OSS 上传失败，请稍后重试", last);
    }
    //update-end---author:cursor---date:2026-08-21---for:【OSS】连接重置时重建客户端并重试，上传带文件大小---

    private String normalizeObjectKey(String objectKey) {
        if (oConvertUtils.isEmpty(objectKey)) {
            throw new IllegalArgumentException("objectKey 不能为空");
        }
        String key = objectKey.replace("\\", "/");
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        if (key.startsWith("upload/")) {
            key = key.substring("upload/".length());
        }
        //update-begin---author:cursor ---date:2026-08-13 for：【上传优化】路径穿越纵深防护，禁止 objectKey 含 .. 段-----------
        for (String seg : key.split("/")) {
            if ("..".equals(seg)) {
                throw new IllegalArgumentException("非法的文件路径");
            }
        }
        //update-end---author:cursor ---date:2026-08-13 for：【上传优化】路径穿越纵深防护-----------
        return key;
    }
}
