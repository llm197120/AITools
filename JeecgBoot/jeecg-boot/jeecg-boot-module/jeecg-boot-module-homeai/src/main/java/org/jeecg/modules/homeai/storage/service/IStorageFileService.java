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

    /** 获取根目录文件列表（folderId 为空，含家庭共享） */
    List<StorageFile> getRootFiles(String userId, String familyId);

    /** 管理端：全部根目录文件 */
    List<StorageFile> getAllRootFiles();

    /** 上传文件 */
    StorageFile uploadFile(String userId, String familyId, String folderId,
                           MultipartFile file, String visibility, String fileName);

    /** 软删除文件 */
    void softDelete(String id);

    /** 软删除指定文件夹下的全部文件（不含子文件夹） */
    void softDeleteByFolderId(String folderId);

    /** 收藏/取消收藏 */
    void toggleFavorite(String id);

    /** 搜索文件（含家庭共享） */
    List<StorageFile> searchFiles(String keyword, String userId, String familyId);

    /** 管理端：搜索全部文件 */
    List<StorageFile> searchAllFiles(String keyword);

    /** 获取文件类型图标 */
    String getFileIcon(String extension);
}
