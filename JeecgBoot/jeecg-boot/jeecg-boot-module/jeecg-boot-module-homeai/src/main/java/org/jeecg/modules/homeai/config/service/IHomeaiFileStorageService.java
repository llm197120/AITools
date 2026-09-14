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

    /**
     * 把存储对象流式写入 HTTP 响应（OSS 边下边写，避免先整包落地再输出导致客户端一直停在「下载中」）。
     */
    void writeToResponse(String storedReference, jakarta.servlet.http.HttpServletResponse response,
                         String downloadName, String extension) throws java.io.IOException;

    /**
     * 同 {@link #writeToResponse(String, jakarta.servlet.http.HttpServletResponse, String, String)}，
     * 若 cacheFile 非空则在写出的同时落一份完整本地副本（仅完整成功后替换），供下次免 OSS。
     */
    void writeToResponse(String storedReference, jakarta.servlet.http.HttpServletResponse response,
                         String downloadName, String extension, java.nio.file.Path cacheFile)
            throws java.io.IOException;

    void deleteIfExists(String storedReference);

    /** 从持久化引用或历史 URL 解析 OSS objectKey */
    String extractObjectKey(String storedReference);
}
