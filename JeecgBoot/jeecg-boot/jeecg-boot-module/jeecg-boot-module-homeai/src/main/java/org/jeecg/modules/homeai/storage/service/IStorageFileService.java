package org.jeecg.modules.homeai.storage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

/**
 * 文件记录 Service
 */
public interface IStorageFileService extends IService<StorageFile> {

    /** 获取文件夹下的文件列表 */
    List<StorageFile> getFilesByFolder(String folderId);

    /** 分页：文件夹内文件 */
    IPage<StorageFile> pageFilesByFolder(Page<StorageFile> page, String folderId);

    /** 获取根目录文件列表（folderId 为空，含家庭共享） */
    List<StorageFile> getRootFiles(String userId, String familyId);

    /** 分页：根目录文件（含家庭共享） */
    IPage<StorageFile> pageRootFiles(Page<StorageFile> page, String userId, String familyId);

    /** 管理端：全部根目录文件 */
    List<StorageFile> getAllRootFiles();

    /** 管理端分页：全部根目录文件 */
    IPage<StorageFile> pageAllRootFiles(Page<StorageFile> page);

    /** 上传文件 */
    StorageFile uploadFile(String userId, String familyId, String folderId,
                           MultipartFile file, String visibility, String fileName);

    /** 软删除文件（进回收站，保留家庭关联以便恢复） */
    void softDelete(String id);

    /** 软删除指定文件夹下的全部文件（不含子文件夹） */
    void softDeleteByFolderId(String folderId);

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R22】资料文件回收站-----------
    /** 回收站分页 */
    IPage<StorageFile> pageRecycleBin(Page<StorageFile> page, String keyword);

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R24】用户侧回收站-----------
    IPage<StorageFile> pageMyRecycleBin(Page<StorageFile> page, String userId, String keyword);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R24】用户侧回收站-----------

    /** 从回收站恢复 */
    void restoreFiles(Collection<String> ids);

    /** 彻底删除（物理删库 + 存储对象 + 家庭关联） */
    void deletePermanently(Collection<String> ids);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R22】资料文件回收站-----------

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】用户存储用量-----------
    /** 用户已用空间（仅未删除文件） */
    long sumUsedBytesByUser(String userId);

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭存储用量-----------
    /** 家庭已用空间：家庭成员上传文件之和（未删除） */
    long sumUsedBytesByFamily(String familyId);

    //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R32】家庭配额运营看板-----------
    /** 家庭配额看板：用量 + 限额 + 告警（可按关键词/告警/自定义过滤） */
    List<java.util.Map<String, Object>> listFamilyQuotaBoard(String keyword, Boolean onlyWarn, Boolean onlyCustom);
    //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R32】家庭配额运营看板-----------
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭存储用量-----------
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】用户存储用量-----------

    /** 收藏/取消收藏 */
    void toggleFavorite(String id);

    /** 搜索文件（含家庭共享） */
    List<StorageFile> searchFiles(String keyword, String userId, String familyId);

    /** 管理端：搜索全部文件 */
    List<StorageFile> searchAllFiles(String keyword);

    /** 获取文件类型图标 */
    String getFileIcon(String extension);
}
