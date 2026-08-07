package org.jeecg.modules.homeai.storage.service;

import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.jeecg.modules.homeai.storage.entity.StorageFolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 资料存储可见家庭关联
 */
public interface IStorageResourceFamilyService {

    List<String> getFolderFamilyIds(String folderId);

    List<String> getFileFamilyIds(String fileId);

    Map<String, List<String>> getFolderFamilyIdsBatch(Collection<String> folderIds);

    Map<String, List<String>> getFileFamilyIdsBatch(Collection<String> fileIds);

    void replaceFolderFamilies(String folderId, List<String> familyIds);

    void replaceFileFamilies(String fileId, List<String> familyIds);

    void deleteByFolderId(String folderId);

    void deleteByFileId(String fileId);

    void enrichFolder(StorageFolder folder);

    void enrichFolders(List<StorageFolder> folders);

    void enrichFile(StorageFile file);

    void enrichFiles(List<StorageFile> files);

    boolean isFolderVisibleToFamily(String folderId, String userFamilyId, String legacyFamilyId);

    boolean isFileVisibleToFamily(String fileId, String userFamilyId, String legacyFamilyId);
}
