package org.jeecg.modules.homeai.storage.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.storage.constant.StorageVisibility;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.jeecg.modules.homeai.storage.entity.StorageFolder;

/**
 * 资料存储列表可见性 SQL 条件
 */
public final class StorageVisibilityQueryUtil {

    private StorageVisibilityQueryUtil() {
    }

    public static void applyReadableFileFilter(LambdaQueryWrapper<StorageFile> query, String userId, String userFamilyId) {
        query.and(w -> {
            w.eq(StorageFile::getUserId, userId)
                    .or()
                    .eq(StorageFile::getVisibility, StorageVisibility.PUBLIC);
            if (oConvertUtils.isNotEmpty(userFamilyId)) {
                w.or(o -> o.eq(StorageFile::getVisibility, StorageVisibility.FAMILY)
                        .and(sub -> sub.eq(StorageFile::getFamilyId, userFamilyId)
                                .or()
                                .apply("EXISTS (SELECT 1 FROM homeai_storage_file_family sf "
                                        + "WHERE sf.file_id = homeai_storage_file.id AND sf.family_id = {0})", userFamilyId)));
            }
        });
    }

    public static void applyReadableFolderFilter(LambdaQueryWrapper<StorageFolder> query, String userId, String userFamilyId) {
        query.and(w -> {
            w.eq(StorageFolder::getUserId, userId)
                    .or()
                    .eq(StorageFolder::getVisibility, StorageVisibility.PUBLIC);
            if (oConvertUtils.isNotEmpty(userFamilyId)) {
                w.or(o -> o.eq(StorageFolder::getVisibility, StorageVisibility.FAMILY)
                        .and(sub -> sub.eq(StorageFolder::getFamilyId, userFamilyId)
                                .or()
                                .apply("EXISTS (SELECT 1 FROM homeai_storage_folder_family sf "
                                        + "WHERE sf.folder_id = homeai_storage_folder.id AND sf.family_id = {0})", userFamilyId)));
            }
        });
    }
}
