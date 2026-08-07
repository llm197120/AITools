package org.jeecg.modules.homeai.storage.util;



import org.jeecg.common.util.oConvertUtils;

import org.jeecg.modules.homeai.storage.constant.StorageVisibility;

import org.jeecg.modules.homeai.storage.entity.StorageFile;

import org.jeecg.modules.homeai.storage.entity.StorageFolder;

import org.jeecg.modules.homeai.storage.service.IStorageResourceFamilyService;



import java.util.List;



/**

 * 资料存储可见性与写权限判断

 * <p>

 * private：仅创建者/上传者；family：关联家庭成员；public：所有已登录用户；管理员可 bypass。

 * </p>

 */

public final class StorageAccessUtil {



    private StorageAccessUtil() {

    }



    public static boolean canAccessFolder(String userId, String userFamilyId, StorageFolder folder,

                                          IStorageResourceFamilyService familyService) {

        if (folder == null || oConvertUtils.isEmpty(userId)) {

            return false;

        }

        if (userId.equals(folder.getUserId())) {

            return true;

        }

        if (StorageVisibility.PUBLIC.equals(folder.getVisibility())) {

            return true;

        }

        if (StorageVisibility.FAMILY.equals(folder.getVisibility())) {

            return familyService.isFolderVisibleToFamily(folder.getId(), userFamilyId, folder.getFamilyId());

        }

        return false;

    }



    public static boolean canAccessFile(String userId, String userFamilyId, StorageFile file, StorageFolder folder,

                                        IStorageResourceFamilyService familyService) {

        if (file == null || oConvertUtils.isEmpty(userId)) {

            return false;

        }

        if (userId.equals(file.getUserId())) {

            return true;

        }

        if (StorageVisibility.PUBLIC.equals(file.getVisibility())) {

            return true;

        }

        if (StorageVisibility.FAMILY.equals(file.getVisibility())

                && familyService.isFileVisibleToFamily(file.getId(), userFamilyId, file.getFamilyId())) {

            return true;

        }

        return canAccessFolder(userId, userFamilyId, folder, familyService);

    }



    public static boolean canWriteFile(String userId, StorageFile file) {

        return file != null && userId != null && userId.equals(file.getUserId());

    }



    public static boolean canWriteFolder(String userId, StorageFolder folder) {

        return folder != null && userId != null && userId.equals(folder.getUserId());

    }



    /** 解析 familyIds 请求参数（逗号分隔） */

    public static List<String> parseFamilyIds(String familyIdsParam) {

        if (oConvertUtils.isEmpty(familyIdsParam)) {

            return List.of();

        }

        return java.util.Arrays.stream(familyIdsParam.split(","))

                .map(String::trim)

                .filter(oConvertUtils::isNotEmpty)

                .distinct()

                .toList();

    }

}


