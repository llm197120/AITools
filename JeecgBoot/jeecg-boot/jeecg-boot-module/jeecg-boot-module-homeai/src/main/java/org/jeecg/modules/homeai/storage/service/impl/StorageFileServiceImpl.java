package org.jeecg.modules.homeai.storage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.config.HomeaiFileUrlUtil;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.jeecg.modules.homeai.storage.mapper.StorageFileMapper;
import org.jeecg.modules.homeai.storage.service.IStorageFileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
public class StorageFileServiceImpl extends ServiceImpl<StorageFileMapper, StorageFile>
        implements IStorageFileService {

    @Value("${jeecg.path.upload:./upload}")
    private String uploadPath;

    @Override
    public List<StorageFile> getFilesByFolder(String folderId) {
        LambdaQueryWrapper<StorageFile> query = new LambdaQueryWrapper<>();
        query.eq(StorageFile::getFolderId, folderId)
                .eq(StorageFile::getDelFlag, 0)
                .orderByDesc(StorageFile::getCreateTime);
        return list(query);
    }

    @Override
    public StorageFile uploadFile(String userId, String familyId, String folderId,
                                   MultipartFile file, String visibility) {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase() : "";
        if (!HomeaiFileUrlUtil.isAllowedUploadExtension(ext)) {
            throw new RuntimeException("不支持上传该文件类型");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);

        // 保存物理文件到上传目录：{upload}/homeai/{userId}/{storedName}
        try {
            String dir = uploadPath + "/homeai/" + userId + "/";
            Files.createDirectories(Paths.get(dir));
            file.transferTo(Paths.get(dir + storedName).toFile());
        } catch (IOException e) {
            log.error("文件保存失败: userId={}, name={}", userId, originalName, e);
            throw new RuntimeException("文件保存失败", e);
        }

        // 数据库保存文件绝对访问地址
        String fileUrl = HomeaiFileUrlUtil.toAbsoluteUrl("/upload/homeai/" + userId + "/" + storedName);

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
        sf.setVisibility(visibility != null ? visibility : "private");
        sf.setIsFavorite("0");
        sf.setDownloadCount(0);
        sf.setCreateTime(new Date());
        sf.setUpdateTime(new Date());
        save(sf);
        return sf;
    }

    @Override
    public void softDelete(String id) {
        // @TableLogic 字段不参与 updateById，需通过 update wrapper 显式设置
        update(new LambdaUpdateWrapper<StorageFile>()
                .eq(StorageFile::getId, id)
                .set(StorageFile::getDelFlag, 1)
                .set(StorageFile::getDeletedAt, new Date()));
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
    public List<StorageFile> searchFiles(String keyword, String userId) {
        LambdaQueryWrapper<StorageFile> query = new LambdaQueryWrapper<>();
        query.eq(StorageFile::getUserId, userId)
                .eq(StorageFile::getDelFlag, 0)
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
