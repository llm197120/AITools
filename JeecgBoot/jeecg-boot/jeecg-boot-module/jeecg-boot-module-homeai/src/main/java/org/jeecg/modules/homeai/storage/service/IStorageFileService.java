package org.jeecg.modules.homeai.storage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件记录 Service
 */
public interface IStorageFileService extends IService<StorageFile> {

    /** 获取文件夹下的文件列表 */
    List<StorageFile> getFilesByFolder(String folderId);

    /** 上传文件 */
    StorageFile uploadFile(String userId, String familyId, String folderId,
                           MultipartFile file, String visibility);

    /** 软删除文件 */
    void softDelete(String id);

    /** 收藏/取消收藏 */
    void toggleFavorite(String id);

    /** 搜索文件 */
    List<StorageFile> searchFiles(String keyword, String userId);

    /** 获取文件类型图标 */
    String getFileIcon(String extension);
}
