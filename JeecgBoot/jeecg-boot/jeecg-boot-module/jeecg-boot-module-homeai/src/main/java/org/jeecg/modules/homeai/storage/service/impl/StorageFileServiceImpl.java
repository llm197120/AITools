package org.jeecg.modules.homeai.storage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.HomeaiFileMagicUtil;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.config.service.IHomeaiFileWhitelistService;
import org.jeecg.modules.homeai.storage.constant.StorageVisibility;
import org.jeecg.modules.homeai.storage.util.StorageFileNameUtil;
import org.jeecg.modules.homeai.storage.util.StorageVisibilityQueryUtil;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.jeecg.modules.homeai.storage.mapper.StorageFileMapper;
import org.jeecg.modules.homeai.storage.service.IStorageFileService;
import org.jeecg.modules.homeai.storage.service.IStorageResourceFamilyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class StorageFileServiceImpl extends ServiceImpl<StorageFileMapper, StorageFile>
        implements IStorageFileService {


    @Autowired
    private IHomeaiFileWhitelistService whitelistService;

    @Autowired
    private IHomeaiFileStorageService fileStorageService;

    @Autowired
    private IStorageResourceFamilyService resourceFamilyService;

    @Override
    public List<StorageFile> getFilesByFolder(String folderId) {
        LambdaQueryWrapper<StorageFile> query = new LambdaQueryWrapper<>();
        query.eq(StorageFile::getFolderId, folderId)
                .eq(StorageFile::getDelFlag, 0)
                .orderByDesc(StorageFile::getCreateTime);
        return list(query);
    }

    @Override
    public List<StorageFile> getRootFiles(String userId, String familyId) {
        LambdaQueryWrapper<StorageFile> query = new LambdaQueryWrapper<>();
        query.eq(StorageFile::getDelFlag, 0)
                .and(w -> w.isNull(StorageFile::getFolderId).or().eq(StorageFile::getFolderId, ""));
        StorageVisibilityQueryUtil.applyReadableFileFilter(query, userId, familyId);
        query.orderByDesc(StorageFile::getCreateTime);
        return list(query);
    }

    @Override
    public List<StorageFile> getAllRootFiles() {
        LambdaQueryWrapper<StorageFile> query = new LambdaQueryWrapper<>();
        query.eq(StorageFile::getDelFlag, 0)
                .and(w -> w.isNull(StorageFile::getFolderId).or().eq(StorageFile::getFolderId, ""))
                .orderByDesc(StorageFile::getCreateTime);
        return list(query);
    }

    @Override
    public StorageFile uploadFile(String userId, String familyId, String folderId,
                                   MultipartFile file, String visibility, String fileName) {
        String originalName = resolveOriginalName(file, fileName);
        String ext = StorageFileNameUtil.extensionOf(originalName);
        if (!whitelistService.isAllowedExtension(ext)) {
            throw new RuntimeException("不支持上传该文件类型");
        }
        try {
            HomeaiFileMagicUtil.validate(file, ext);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        // storedName：OSS/磁盘实际文件名（UUID），与 originalName 分字段存储
        String storedName = UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);
        String objectKey = StorageFileNameUtil.buildObjectKey(userId, storedName);
        String fileUrl = fileStorageService.storeMultipart(file, objectKey);

        StorageFile sf = new StorageFile();
        sf.setUserId(userId);
        sf.setFamilyId(familyId);
        sf.setFolderId(folderId);
        sf.setOriginalName(originalName);
        sf.setStoredName(storedName);
        sf.setExtension(ext);
        sf.setMimeType(file.getContentType());
        sf.setFileSize(file.getSize());
        sf.setFileUrl(fileUrl);
        sf.setVisibility(visibility != null ? visibility : StorageVisibility.PRIVATE);
        sf.setIsFavorite("0");
        sf.setDownloadCount(0);
        sf.setCreateTime(new Date());
        sf.setUpdateTime(new Date());
        save(sf);
        return sf;
    }

    /** 优先使用客户端传入的原始文件名；storedName 单独生成，二者分字段落库 */
    private String resolveOriginalName(MultipartFile file, String fileName) {
        if (oConvertUtils.isNotEmpty(fileName)) {
            return StorageFileNameUtil.sanitizeOriginalName(fileName);
        }
        String fromMultipart = file.getOriginalFilename();
        if (StorageFileNameUtil.isTempUploadName(fromMultipart)) {
            String ext = StorageFileNameUtil.extensionOf(fromMultipart);
            if (oConvertUtils.isEmpty(ext)) {
                ext = "dat";
            }
            return "FILE_" + System.currentTimeMillis() + "." + ext;
        }
        return StorageFileNameUtil.sanitizeOriginalName(fromMultipart);
    }

    @Override
    public void softDelete(String id) {
        update(new LambdaUpdateWrapper<StorageFile>()
                .eq(StorageFile::getId, id)
                .set(StorageFile::getDelFlag, 1)
                .set(StorageFile::getDeletedAt, new Date()));
        resourceFamilyService.deleteByFileId(id);
    }

    @Override
    public void softDeleteByFolderId(String folderId) {
        if (oConvertUtils.isEmpty(folderId)) {
            return;
        }
        List<StorageFile> files = list(new LambdaQueryWrapper<StorageFile>()
                .eq(StorageFile::getFolderId, folderId)
                .eq(StorageFile::getDelFlag, 0));
        update(new LambdaUpdateWrapper<StorageFile>()
                .eq(StorageFile::getFolderId, folderId)
                .eq(StorageFile::getDelFlag, 0)
                .set(StorageFile::getDelFlag, 1)
                .set(StorageFile::getDeletedAt, new Date()));
        for (StorageFile file : files) {
            resourceFamilyService.deleteByFileId(file.getId());
        }
    }

    @Override
    public void toggleFavorite(String id) {
        StorageFile sf = getById(id);
        if (sf != null) {
            sf.setIsFavorite("1".equals(sf.getIsFavorite()) ? "0" : "1");
            updateById(sf);
        }
    }

    @Override
    public List<StorageFile> searchFiles(String keyword, String userId, String familyId) {
        LambdaQueryWrapper<StorageFile> query = new LambdaQueryWrapper<>();
        query.eq(StorageFile::getDelFlag, 0)
                .like(StorageFile::getOriginalName, keyword);
        StorageVisibilityQueryUtil.applyReadableFileFilter(query, userId, familyId);
        query.orderByDesc(StorageFile::getCreateTime);
        return list(query);
    }

    @Override
    public List<StorageFile> searchAllFiles(String keyword) {
        LambdaQueryWrapper<StorageFile> query = new LambdaQueryWrapper<>();
        query.eq(StorageFile::getDelFlag, 0)
                .like(StorageFile::getOriginalName, keyword)
                .orderByDesc(StorageFile::getCreateTime);
        return list(query);
    }

    @Override
    public String getFileIcon(String extension) {
        Map<String, String> iconMap = new HashMap<>();
        iconMap.put("jpg", "image");
        iconMap.put("jpeg", "image");
        iconMap.put("png", "image");
        iconMap.put("gif", "image");
        iconMap.put("bmp", "image");
        iconMap.put("webp", "image");
        iconMap.put("pdf", "pdf");
        iconMap.put("doc", "word");
        iconMap.put("docx", "word");
        iconMap.put("xls", "excel");
        iconMap.put("xlsx", "excel");
        iconMap.put("ppt", "ppt");
        iconMap.put("pptx", "ppt");
        iconMap.put("mp4", "video");
        iconMap.put("avi", "video");
        iconMap.put("mov", "video");
        iconMap.put("zip", "archive");
        iconMap.put("rar", "archive");
        iconMap.put("7z", "archive");
        iconMap.put("txt", "text");
        iconMap.put("md", "text");
        return iconMap.getOrDefault(extension, "file");
    }
}
