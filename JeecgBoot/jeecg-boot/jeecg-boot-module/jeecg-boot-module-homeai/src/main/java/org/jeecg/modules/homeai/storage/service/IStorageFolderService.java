package org.jeecg.modules.homeai.storage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.storage.entity.StorageFolder;

import java.util.Collection;
import java.util.List;

/**
 * 文件夹 Service
 */
public interface IStorageFolderService extends IService<StorageFolder> {

    /** 获取用户的文件夹树 */
    List<StorageFolder> getUserFolderTree(String userId, String familyId);

    /** 管理端：全部文件夹树 */
    List<StorageFolder> getAllFolderTree();

    /** 获取根目录（parent_id IS NULL） */
    StorageFolder getRootFolder(String userId, String familyId);

    /** 创建文件夹 */
    StorageFolder createFolder(String userId, String familyId, String parentId, String name, String visibility);

    /** 更新文件夹（含父级变更校验） */
    void updateFolder(StorageFolder folder, String newParentId);

    /** 校验上级文件夹不会形成循环引用 */
    void validateParentNotCycle(String folderId, String parentId);

    /** 获取子文件夹 */
    List<StorageFolder> getChildFolders(String parentId);

    /** 递归软删文件夹及其内所有文件、子文件夹（移入回收站） */
    void deleteFolderCascade(String folderId);

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】文件夹回收站-----------
    IPage<StorageFolder> pageRecycleBin(Page<StorageFolder> page, String keyword);

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R24】用户侧文件夹回收站-----------
    IPage<StorageFolder> pageMyRecycleBin(Page<StorageFolder> page, String userId, String keyword);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R24】用户侧文件夹回收站-----------

    void restoreFolders(Collection<String> ids);

    void deleteFoldersPermanently(Collection<String> ids);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】文件夹回收站-----------
}
