package org.jeecg.modules.homeai.storage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.jeecg.modules.homeai.storage.entity.StorageFileFamily;
import org.jeecg.modules.homeai.storage.entity.StorageFolder;
import org.jeecg.modules.homeai.storage.entity.StorageFolderFamily;
import org.jeecg.modules.homeai.storage.mapper.StorageFileFamilyMapper;
import org.jeecg.modules.homeai.storage.mapper.StorageFolderFamilyMapper;
import org.jeecg.modules.homeai.storage.service.IStorageResourceFamilyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StorageResourceFamilyServiceImpl implements IStorageResourceFamilyService {

    @Autowired
    private StorageFolderFamilyMapper folderFamilyMapper;

    @Autowired
    private StorageFileFamilyMapper fileFamilyMapper;

    @Override
    public List<String> getFolderFamilyIds(String folderId) {
        if (oConvertUtils.isEmpty(folderId)) {
            return Collections.emptyList();
        }
        return folderFamilyMapper.selectList(new LambdaQueryWrapper<StorageFolderFamily>()
                        .eq(StorageFolderFamily::getFolderId, folderId))
                .stream()
                .map(StorageFolderFamily::getFamilyId)
                .filter(oConvertUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFileFamilyIds(String fileId) {
        if (oConvertUtils.isEmpty(fileId)) {
            return Collections.emptyList();
        }
        return fileFamilyMapper.selectList(new LambdaQueryWrapper<StorageFileFamily>()
                        .eq(StorageFileFamily::getFileId, fileId))
                .stream()
                .map(StorageFileFamily::getFamilyId)
                .filter(oConvertUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<String>> getFolderFamilyIdsBatch(Collection<String> folderIds) {
        if (folderIds == null || folderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<StorageFolderFamily> rows = folderFamilyMapper.selectList(new LambdaQueryWrapper<StorageFolderFamily>()
                .in(StorageFolderFamily::getFolderId, folderIds));
        Map<String, List<String>> map = new HashMap<>();
        for (StorageFolderFamily row : rows) {
            map.computeIfAbsent(row.getFolderId(), k -> new ArrayList<>()).add(row.getFamilyId());
        }
        return map;
    }

    @Override
    public Map<String, List<String>> getFileFamilyIdsBatch(Collection<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<StorageFileFamily> rows = fileFamilyMapper.selectList(new LambdaQueryWrapper<StorageFileFamily>()
                .in(StorageFileFamily::getFileId, fileIds));
        Map<String, List<String>> map = new HashMap<>();
        for (StorageFileFamily row : rows) {
            map.computeIfAbsent(row.getFileId(), k -> new ArrayList<>()).add(row.getFamilyId());
        }
        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceFolderFamilies(String folderId, List<String> familyIds) {
        deleteByFolderId(folderId);
        insertFolderFamilies(folderId, familyIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceFileFamilies(String fileId, List<String> familyIds) {
        deleteByFileId(fileId);
        insertFileFamilies(fileId, familyIds);
    }

    @Override
    public void deleteByFolderId(String folderId) {
        if (oConvertUtils.isEmpty(folderId)) {
            return;
        }
        folderFamilyMapper.delete(new LambdaQueryWrapper<StorageFolderFamily>()
                .eq(StorageFolderFamily::getFolderId, folderId));
    }

    @Override
    public void deleteByFileId(String fileId) {
        if (oConvertUtils.isEmpty(fileId)) {
            return;
        }
        fileFamilyMapper.delete(new LambdaQueryWrapper<StorageFileFamily>()
                .eq(StorageFileFamily::getFileId, fileId));
    }

    @Override
    public void enrichFolder(StorageFolder folder) {
        if (folder == null) {
            return;
        }
        folder.setFamilyIds(getFolderFamilyIds(folder.getId()));
    }

    @Override
    public void enrichFolders(List<StorageFolder> folders) {
        if (folders == null || folders.isEmpty()) {
            return;
        }
        List<String> ids = folders.stream().map(StorageFolder::getId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<String, List<String>> map = getFolderFamilyIdsBatch(ids);
        for (StorageFolder folder : folders) {
            folder.setFamilyIds(map.getOrDefault(folder.getId(), Collections.emptyList()));
        }
    }

    @Override
    public void enrichFile(StorageFile file) {
        if (file == null) {
            return;
        }
        file.setFamilyIds(getFileFamilyIds(file.getId()));
    }

    @Override
    public void enrichFiles(List<StorageFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        List<String> ids = files.stream().map(StorageFile::getId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<String, List<String>> map = getFileFamilyIdsBatch(ids);
        for (StorageFile file : files) {
            file.setFamilyIds(map.getOrDefault(file.getId(), Collections.emptyList()));
        }
    }

    @Override
    public boolean isFolderVisibleToFamily(String folderId, String userFamilyId, String legacyFamilyId) {
        if (oConvertUtils.isEmpty(userFamilyId)) {
            return false;
        }
        if (userFamilyId.equals(legacyFamilyId)) {
            return true;
        }
        return getFolderFamilyIds(folderId).contains(userFamilyId);
    }

    @Override
    public boolean isFileVisibleToFamily(String fileId, String userFamilyId, String legacyFamilyId) {
        if (oConvertUtils.isEmpty(userFamilyId)) {
            return false;
        }
        if (userFamilyId.equals(legacyFamilyId)) {
            return true;
        }
        return getFileFamilyIds(fileId).contains(userFamilyId);
    }

    private void insertFolderFamilies(String folderId, List<String> familyIds) {
        if (oConvertUtils.isEmpty(folderId) || familyIds == null) {
            return;
        }
        Date now = new Date();
        for (String familyId : distinctNonEmpty(familyIds)) {
            StorageFolderFamily row = new StorageFolderFamily();
            row.setFolderId(folderId);
            row.setFamilyId(familyId);
            row.setCreateTime(now);
            folderFamilyMapper.insert(row);
        }
    }

    private void insertFileFamilies(String fileId, List<String> familyIds) {
        if (oConvertUtils.isEmpty(fileId) || familyIds == null) {
            return;
        }
        Date now = new Date();
        for (String familyId : distinctNonEmpty(familyIds)) {
            StorageFileFamily row = new StorageFileFamily();
            row.setFileId(fileId);
            row.setFamilyId(familyId);
            row.setCreateTime(now);
            fileFamilyMapper.insert(row);
        }
    }

    private List<String> distinctNonEmpty(List<String> familyIds) {
        return familyIds.stream()
                .filter(oConvertUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
    }
}
