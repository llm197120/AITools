package org.jeecg.modules.homeai.storage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.storage.entity.StorageFolder;

import java.util.List;

/**
 * 文件夹 Service
 */
public interface IStorageFolderService extends IService<StorageFolder> {

    /** 获取用户的文件夹树 */
    List<StorageFolder> getUserFolderTree(String userId, String familyId);

    /** 获取根目录（parent_id IS NULL） */
    StorageFolder getRootFolder(String userId, String familyId);

    /** 创建文件夹 */
    StorageFolder createFolder(String userId, String familyId, String parentId, String name, String visibility);

    /** 获取子文件夹 */
    List<StorageFolder> getChildFolders(String parentId);
}
