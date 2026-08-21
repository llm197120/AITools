package org.jeecg.common.util.oss;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.Protocol;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItemStream;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.constant.SymbolConstant;
import org.jeecg.common.util.CommonUtils;
import org.jeecg.common.util.filter.SsrfFileTypeFilter;
import org.jeecg.common.util.filter.StrAttackFilter;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;
import java.util.UUID;

/**
 * @Description: 阿里云 oss 上传工具类(高依赖版)
 * @Date: 2019/5/10
 * @author: jeecg-boot
 */
@Slf4j
public class OssBootUtil {

    private static String endPoint;
    private static String accessKeyId;
    private static String accessKeySecret;
    private static String bucketName;
    private static String staticDomain;

    public static void setEndPoint(String endPoint) {
        OssBootUtil.endPoint = endPoint;
    }

    public static void setAccessKeyId(String accessKeyId) {
        OssBootUtil.accessKeyId = accessKeyId;
    }

    public static void setAccessKeySecret(String accessKeySecret) {
        OssBootUtil.accessKeySecret = accessKeySecret;
    }

    public static void setBucketName(String bucketName) {
        OssBootUtil.bucketName = bucketName;
    }

    public static void setStaticDomain(String staticDomain) {
        OssBootUtil.staticDomain = staticDomain;
    }

    public static String getStaticDomain() {
        return staticDomain;
    }

    public static String getEndPoint() {
        return endPoint;
    }

    public static String getAccessKeyId() {
        return accessKeyId;
    }

    public static String getAccessKeySecret() {
        return accessKeySecret;
    }

    public static String getBucketName() {
        return bucketName;
    }

    public static OSSClient getOssClient() {
        return ossClient;
    }

    /**
     * oss 工具客户端
     */
    private static OSSClient ossClient = null;

    /**
     * 上传文件至阿里云 OSS
     * 文件上传成功,返回文件完整访问路径
     * 文件上传失败,返回 null
     *
     * @param file    待上传文件
     * @param fileDir 文件保存目录
     * @return oss 中的相对文件路径
     */
    public static String upload(MultipartFile file, String fileDir,String customBucket) throws Exception {
        // 文件安全校验，防止上传漏洞文件
        SsrfFileTypeFilter.checkUploadFileType(file);

        String filePath = null;
        initOss(endPoint, accessKeyId, accessKeySecret);
        StringBuilder fileUrl = new StringBuilder();
        String newBucket = bucketName;
        if(oConvertUtils.isNotEmpty(customBucket)){
            newBucket = customBucket;
        }
        try {
            //判断桶是否存在,不存在则创建桶
            if(!ossClient.doesBucketExist(newBucket)){
                ossClient.createBucket(newBucket);
            }
            // 获取文件名
            String orgName = file.getOriginalFilename();
            if("" == orgName){
              orgName=file.getName();
            }
            orgName = CommonUtils.getFileName(orgName);
            String fileName = orgName.indexOf(".")==-1
                              ?orgName + "_" + System.currentTimeMillis()
                              :orgName.substring(0, orgName.lastIndexOf(".")) + "_" + System.currentTimeMillis() + orgName.substring(orgName.lastIndexOf("."));
            if (!fileDir.endsWith(SymbolConstant.SINGLE_SLASH)) {
                fileDir = fileDir.concat(SymbolConstant.SINGLE_SLASH);
            }
            // 代码逻辑说明: 过滤上传文件夹名特殊字符，防止攻击
            fileDir=StrAttackFilter.filter(fileDir);
            fileUrl = fileUrl.append(fileDir + fileName);

            if (oConvertUtils.isNotEmpty(staticDomain) && staticDomain.toLowerCase().startsWith(CommonConstant.STR_HTTP)) {
                filePath = staticDomain + SymbolConstant.SINGLE_SLASH + fileUrl;
            } else {
                filePath = "https://" + newBucket + "." + endPoint + SymbolConstant.SINGLE_SLASH + fileUrl;
            }
            PutObjectResult result = ossClient.putObject(newBucket, fileUrl.toString(), file.getInputStream());
            // 设置权限(公开读)
//            ossClient.setBucketAcl(newBucket, CannedAccessControlList.PublicRead);
            if (result != null) {
                log.info("------OSS文件上传成功------" + fileUrl);
            }
        } catch (IOException e) {
            log.error(e.getMessage(),e);
            return null;
        }catch (Exception e) {
            log.error(e.getMessage(),e);
            return null;
        }
        return filePath;
    }

    /**
     * 获取原始URL
    * @param url: 原始URL
    * @Return: java.lang.String
    */
    public static String getOriginalUrl(String url) {
        String originalDomain = "https://" + bucketName + "." + endPoint;
        if(oConvertUtils.isNotEmpty(staticDomain) && url.indexOf(staticDomain)!=-1){
            url = url.replace(staticDomain,originalDomain);
        }
        return url;
    }

    /**
     * 文件上传
     * @param file
     * @param fileDir
     * @return
     */
    public static String upload(MultipartFile file, String fileDir) throws Exception {
        return upload(file, fileDir,null);
    }

    /**
     * 上传文件至阿里云 OSS
     * 文件上传成功,返回文件完整访问路径
     * 文件上传失败,返回 null
     *
     * @param file    待上传文件
     * @param fileDir 文件保存目录
     * @return oss 中的相对文件路径
     */
    public static String upload(FileItemStream file, String fileDir) {
        String filePath = null;
        initOss(endPoint, accessKeyId, accessKeySecret);
        StringBuilder fileUrl = new StringBuilder();
        try {
            String suffix = file.getName().substring(file.getName().lastIndexOf('.'));
            String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
            if (!fileDir.endsWith(SymbolConstant.SINGLE_SLASH)) {
                fileDir = fileDir.concat(SymbolConstant.SINGLE_SLASH);
            }
            fileDir = StrAttackFilter.filter(fileDir);
            fileUrl = fileUrl.append(fileDir + fileName);
            if (oConvertUtils.isNotEmpty(staticDomain) && staticDomain.toLowerCase().startsWith(CommonConstant.STR_HTTP)) {
                filePath = staticDomain + SymbolConstant.SINGLE_SLASH + fileUrl;
            } else {
                filePath = "https://" + bucketName + "." + endPoint + SymbolConstant.SINGLE_SLASH + fileUrl;
            }
            PutObjectResult result = ossClient.putObject(bucketName, fileUrl.toString(), file.openStream());
            // 设置权限(公开读)
            ossClient.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);
            if (result != null) {
                log.info("------OSS文件上传成功------" + fileUrl);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return filePath;
    }

    /**
     * 删除文件
     * @param url
     */
    public static void deleteUrl(String url) {
        deleteUrl(url,null);
    }

    /**
     * 删除文件
     * @param url
     */
    public static void deleteUrl(String url,String bucket) {
        String newBucket = bucketName;
        if(oConvertUtils.isNotEmpty(bucket)){
            newBucket = bucket;
        }
        String bucketUrl = "";
        if (oConvertUtils.isNotEmpty(staticDomain) && staticDomain.toLowerCase().startsWith(CommonConstant.STR_HTTP)) {
            bucketUrl = staticDomain + SymbolConstant.SINGLE_SLASH ;
        } else {
            bucketUrl = "https://" + newBucket + "." + endPoint + SymbolConstant.SINGLE_SLASH;
        }
        //TODO 暂时不允许删除云存储的文件
        //initOss(endPoint, accessKeyId, accessKeySecret);
        url = url.replace(bucketUrl,"");
        ossClient.deleteObject(newBucket, url);
    }

    /**
     * 删除文件
     * @param fileName
     */
    public static void delete(String fileName) {
        ossClient.deleteObject(bucketName, fileName);
    }

    /**
     * 获取指定桶（私有桶）中的文件流
     * 通过OSS SDK直接读取文件内容，支持指定自定义桶名（如 "eoafile"），为空则使用默认桶
     *
     * @param objectName 文件对象路径（如 "eoafile/2026/04/test.pdf"，会自动替换前缀）
     * @param bucket     自定义桶名称，为空则使用默认桶
     * @return 文件输入流，失败返回null
     */
    public static InputStream getOssFile(String objectName,String bucket){
        InputStream inputStream = null;
        try{
            String newBucket = bucketName;
            if(oConvertUtils.isNotEmpty(bucket)){
                newBucket = bucket;
            }
            initOss(endPoint, accessKeyId, accessKeySecret);
            // 代码逻辑说明: 替换objectName前缀，防止key不一致导致获取不到文件----
            objectName = OssBootUtil.replacePrefix(objectName,bucket);
            OSSObject ossObject = ossClient.getObject(newBucket,objectName);
            inputStream = new BufferedInputStream(ossObject.getObjectContent());
        }catch (Exception e){
            log.info("文件获取失败" + e.getMessage());
        }
        return inputStream;
    }

    ///**
    // * 获取文件流
    // * @param objectName
    // * @return
    // */
    //public static InputStream getOssFile(String objectName){
    //    return getOssFile(objectName,null);
    //}

    /**
     * 获取私有桶文件的预签名访问URL（带过期时间）
     * 通过OSS预签名机制生成临时访问链接，无需公开桶即可让外部下载/预览文件
     *
     * @param bucketName 桶名称（如 "eoafile"）
     * @param objectName 文件对象路径（会自动替换前缀）
     * @param expires    链接过期时间点（Date类型，如1天后过期）
     * @return 预签名URL字符串，文件不存在或失败返回null
     */
    public static String getObjectUrl(String bucketName, String objectName, Date expires) {
        initOss(endPoint, accessKeyId, accessKeySecret);
        try{
            // 代码逻辑说明: 替换objectName前缀，防止key不一致导致获取不到文件----
            objectName = OssBootUtil.replacePrefix(objectName,bucketName);
            if(ossClient.doesObjectExist(bucketName,objectName)){
                URL url = ossClient.generatePresignedUrl(bucketName,objectName,expires);
                //log.info("原始url : {}", url.toString());
                //log.info("decode url : {}", URLDecoder.decode(url.toString(), "UTF-8"));
                //【issues/4023】问题 oss外链经过转编码后，部分无效，大概在三分一；无需转编码直接返回即可 #4023
                return url.toString();
            }
        }catch (Exception e){
            log.info("文件路径获取失败" + e.getMessage()); 
        }
        return null;
    }

    /**
     * 初始化 oss 客户端
     *
     * @return
     */
    private static synchronized OSSClient initOss(String endpoint, String accessKeyId, String accessKeySecret) {
        if (ossClient == null) {
            //update-begin---author:cursor---date:2026-08-21---for:【OSS】HTTPS + 超时/重试，避免空闲连接被对端 RST---
            ClientConfiguration conf = new ClientConfiguration();
            conf.setProtocol(Protocol.HTTPS);
            conf.setMaxErrorRetry(5);
            conf.setConnectionTimeout(15000);
            conf.setSocketTimeout(60000);
            conf.setIdleConnectionTime(30000);
            conf.setRequestTimeoutEnabled(true);
            conf.setRequestTimeout(60000);
            conf.setSupportCname(false);
            ossClient = new OSSClient(endpoint,
                    new DefaultCredentialProvider(accessKeyId, accessKeySecret),
                    conf);
            //update-end---author:cursor---date:2026-08-21---for:【OSS】HTTPS + 超时/重试，避免空闲连接被对端 RST---
        }
        return ossClient;
    }


    /**
     * 通过输入流上传文件到阿里云OSS默认桶
     * 上传后设置桶为公开读权限，返回文件完整访问URL
     *
     * @param stream       文件输入流
     * @param relativePath 文件在桶中的相对路径（如 "upload/2026/04/test.pdf"）
     * @return 文件完整访问URL（优先使用staticDomain，否则拼接 bucketName.endPoint）
     */
    public static String upload(InputStream stream, String relativePath) {
        String filePath = null;
        String fileUrl = relativePath;
        initOss(endPoint, accessKeyId, accessKeySecret);
        if (oConvertUtils.isNotEmpty(staticDomain) && staticDomain.toLowerCase().startsWith(CommonConstant.STR_HTTP)) {
            filePath = staticDomain + SymbolConstant.SINGLE_SLASH + relativePath;
        } else {
            filePath = "https://" + bucketName + "." + endPoint + SymbolConstant.SINGLE_SLASH + fileUrl;
        }
        PutObjectResult result = ossClient.putObject(bucketName, fileUrl.toString(),stream);
        // 设置权限(公开读)
        ossClient.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);
        if (result != null) {
            log.info("------OSS文件上传成功------" + fileUrl);
        }
        return filePath;
    }

    /**
     * 上传至私有桶（不修改 Bucket ACL），返回对象 Key
     */
    public static String uploadPrivate(InputStream stream, String relativePath) {
        return uploadPrivate(stream, relativePath, -1, null);
    }

    /**
     * 上传至私有桶。必须带 Content-Length：Multipart 流的 available() 往往不是文件大小，
     * OSS 走 chunked 时本机 JDK 容易被对端 Connection reset。
     */
    public static String uploadPrivate(InputStream stream, String relativePath, long contentLength, String contentType) {
        if (oConvertUtils.isEmpty(relativePath)) {
            return null;
        }
        initOss(endPoint, accessKeyId, accessKeySecret);
        //update-begin---author:cursor---date:2026-08-21---for:【OSS文件名】StrAttackFilter 会清掉 . 和 _，导致 png 变成 xxxpng---
        String objectKey = sanitizePrivateObjectKey(relativePath);
        //update-end---author:cursor---date:2026-08-21---for:【OSS文件名】StrAttackFilter 会清掉 . 和 _，导致 png 变成 xxxpng---
        try {
            //update-begin---author:cursor---date:2026-08-21---for:【OSS】去掉每次 doesBucketExist；带 Content-Length 上传---
            ObjectMetadata metadata = new ObjectMetadata();
            if (contentLength > 0) {
                metadata.setContentLength(contentLength);
            }
            if (oConvertUtils.isNotEmpty(contentType)) {
                metadata.setContentType(contentType);
            }
            ossClient.putObject(bucketName, objectKey, stream, metadata);
            //update-end---author:cursor---date:2026-08-21---for:【OSS】去掉每次 doesBucketExist；带 Content-Length 上传---
            log.info("------OSS私有文件上传成功------{}", objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("OSS私有上传失败: {}", e.getMessage(), e);
            throw new RuntimeException(summarizeOssFailure(e), e);
        }
    }

    /**
     * 连接被对端掐掉或超时时，关掉旧客户端，下次 init 会新建连接池。
     */
    public static synchronized void resetClient() {
        if (ossClient == null) {
            return;
        }
        try {
            ossClient.shutdown();
        } catch (Exception e) {
            log.debug("关闭 OSS 客户端: {}", e.getMessage());
        }
        ossClient = null;
    }

    public static boolean isTransientNetworkFailure(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof java.net.SocketException
                    || t instanceof java.net.SocketTimeoutException
                    || t instanceof javax.net.ssl.SSLException
                    || t instanceof ClientException) {
                if (t instanceof ClientException) {
                    String code = ((ClientException) t).getErrorCode();
                    if (code != null && (code.contains("Socket") || code.contains("Timeout") || "Unknown".equals(code))) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
            String msg = t.getMessage();
            if (msg != null) {
                String m = msg.toLowerCase();
                if (m.contains("connection reset")
                        || m.contains("broken pipe")
                        || m.contains("connection refused")
                        || m.contains("timed out")
                        || m.contains("timeout")) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    private static String summarizeOssFailure(Throwable e) {
        if (isTransientNetworkFailure(e)) {
            return "连不上对象存储（连接被重置），请稍后重试";
        }
        String msg = e.getMessage();
        return oConvertUtils.isEmpty(msg) ? "OSS 上传失败" : msg;
    }

    /**
     * 生成私有桶预签名访问 URL
     */
    public static String getPresignedUrl(String objectKey, long expireSeconds) {
        if (oConvertUtils.isEmpty(objectKey) || expireSeconds <= 0) {
            return null;
        }
        Date expires = new Date(System.currentTimeMillis() + expireSeconds * 1000L);
        return getObjectUrl(bucketName, objectKey, expires);
    }

    /**
     * 删除 OSS 对象（按 objectKey）
     */
    public static void deleteObject(String objectKey) {
        if (oConvertUtils.isEmpty(objectKey)) {
            return;
        }
        initOss(endPoint, accessKeyId, accessKeySecret);
        ossClient.deleteObject(bucketName, objectKey);
    }

    /**
     * 替换前缀，防止key不一致导致获取不到文件
     * @param objectName 文件上传路径 key
     * @param customBucket 自定义桶
     * @date 2022-01-20
     * @author lsq
     * @return
     */
    private static String replacePrefix(String objectName,String customBucket){
        log.info("------replacePrefix---替换前---objectName:{}",objectName);
        if(oConvertUtils.isNotEmpty(staticDomain)){
            objectName= objectName.replace(staticDomain+SymbolConstant.SINGLE_SLASH,"");
        }else{
            String newBucket = bucketName;
            if(oConvertUtils.isNotEmpty(customBucket)){
                newBucket = customBucket;
            }
            String path ="https://" + newBucket + "." + endPoint + SymbolConstant.SINGLE_SLASH;
            objectName = objectName.replace(path,"");
        }
        log.info("------replacePrefix---替换后---objectName:{}",objectName);
        return objectName;
    }

    //update-begin---author:cursor---date:2026-08-21---for:【OSS文件名】目录过滤攻击字符，文件名保留扩展名与下划线---
    /**
     * 私有上传 objectKey 清洗：只过滤目录段，文件名保留 `.` `_` `-`，避免扩展名丢失。
     * <p>与 {@code upload(MultipartFile, fileDir)} 一致——后者也只 filter 目录、不碰文件名。
     */
    public static String sanitizePrivateObjectKey(String relativePath) {
        if (oConvertUtils.isEmpty(relativePath)) {
            return relativePath;
        }
        String objectKey = relativePath.replace("\\", "/");
        while (objectKey.startsWith("/")) {
            objectKey = objectKey.substring(1);
        }
        int slash = objectKey.lastIndexOf('/');
        if (slash < 0) {
            return objectKey;
        }
        String dir = StrAttackFilter.filter(objectKey.substring(0, slash + 1));
        String name = objectKey.substring(slash + 1);
        return dir + name;
    }
    //update-end---author:cursor---date:2026-08-21---for:【OSS文件名】目录过滤攻击字符，文件名保留扩展名与下划线---
}