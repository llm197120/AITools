package org.jeecg.modules.homeai.config.service;

import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * HomeAI 文件存储（本地 / 阿里云 OSS，由 jeecg.uploadType 控制）
 */
public interface IHomeaiFileStorageService {

    /** OSS 对象引用前缀，数据库存储格式：oss:homeai/userId/file.ext */
    String OSS_REF_PREFIX = "oss:";

    boolean isOssEnabled();

    boolean isPrivateOssBucket();

    /**
     * @param objectKey 对象路径，如 homeai/{userId}/{fileName}，不含 /upload 前缀
     * @return 持久化引用（OSS 为 oss:key，本地为绝对 URL）
     */
    String storeMultipart(MultipartFile file, String objectKey);

    /** 将已存在的本地文件同步到存储 */
    String storeLocalFile(Path localFile, String objectKey);

    /** 将客户端传入的 URL 规范化为持久化引用（预签名 URL → oss:key） */
    String normalizeStoredReference(String url);

    /** 将持久化引用转为客户端可访问 URL（私有 OSS 返回预签名 URL） */
    String resolveAccessUrl(String storedReference);

    /**
     * 可访问 URL。imageProcess 为阿里云图片处理（如 {@link org.jeecg.modules.homeai.config.HomeaiImageProcess#THUMB}），
     * 非图片或 process 为空时与 {@link #resolveAccessUrl(String)} 相同。
     */
    String resolveAccessUrl(String storedReference, String imageProcess);

    void applyAccessUrl(StorageFile file);

    void applyAccessUrls(List<StorageFile> files);

    /** Office 转换等需要本地 Path 的场景（OSS 时会下载到临时文件） */
    Path resolveLocalPath(String storedReference);

    void deleteIfExists(String storedReference);

    /** 从持久化引用或历史 URL 解析 OSS objectKey */
    String extractObjectKey(String storedReference);
}
