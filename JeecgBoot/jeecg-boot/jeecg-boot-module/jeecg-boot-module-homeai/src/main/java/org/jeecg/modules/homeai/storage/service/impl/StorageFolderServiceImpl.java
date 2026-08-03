package org.jeecg.modules.homeai.storage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.jeecg.modules.homeai.storage.entity.StorageFolder;
import org.jeecg.modules.homeai.storage.mapper.StorageFileMapper;
import org.jeecg.modules.homeai.storage.mapper.StorageFolderMapper;
import org.jeecg.modules.homeai.storage.service.IStorageFolderService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StorageFolderServiceImpl extends ServiceImpl<StorageFolderMapper, StorageFolder>
        implements IStorageFolderService {

    @org.springframework.beans.factory.annotation.Autowired
    private StorageFileMapper fileMapper;

    @Override
    public List<StorageFolder> getUserFolderTree(String userId, String familyId) {
        LambdaQueryWrapper<StorageFolder> query = new LambdaQueryWrapper<>();
        query.eq(StorageFolder::getUserId, userId)
                .eq(StorageFolder::getDelFlag, 0);
        List<StorageFolder> allFolders = list(query);
        // 填充每个文件夹的文件数（用于前端展示）
        for (StorageFolder folder : allFolders) {
            folder.setFileCount(Math.toIntExact(fileMapper.selectCount(
                    new LambdaQueryWrapper<StorageFile>()
                            .eq(StorageFile::getFolderId, folder.getId())
                            .eq(StorageFile::getDelFlag, 0))));
        }
        return buildTree(allFolders, null);
    }

    @Override
    public StorageFolder getRootFolder(String userId, String familyId) {
        LambdaQueryWrapper<StorageFolder> query = new LambdaQueryWrapper<>();
        query.eq(StorageFolder::getUserId, userId)
                .isNull(StorageFolder::getParentId)
                .eq(StorageFolder::getDelFlag, 0)
                .last("LIMIT 1");
        return getOne(query);
    }

    @Override
    public StorageFolder createFolder(String userId, String familyId, String parentId,
                                       String name, String visibility) {
        StorageFolder folder = new StorageFolder();
        folder.setUserId(userId);
        folder.setFamilyId(familyId);
        folder.setParentId(parentId);
        folder.setName(name);
        folder.setVisibility(visibility != null ? visibility : "private");
        folder.setLevel(parentId != null ? getParentLevel(parentId) + 1 : 0);
        folder.setCreateTime(new Date());
        folder.setUpdateTime(new Date());
        save(folder);
        return folder;
    }

    @Override
    public List<StorageFolder> getChildFolders(String parentId) {
        LambdaQueryWrapper<StorageFolder> query = new LambdaQueryWrapper<>();
        query.eq(StorageFolder::getParentId, parentId)
                .eq(StorageFolder::getDelFlag, 0)
                .orderByAsc(StorageFolder::getCreateTime);
        return list(query);
    }

    private int getParentLevel(String parentId) {
        StorageFolder parent = getById(parentId);
        return parent != null && parent.getLevel() != null ? parent.getLevel() : 0;
    }

    private List<StorageFolder> buildTree(List<StorageFolder> all, String parentId) {
        return all.stream()
                .filter(f -> (parentId == null && f.getParentId() == null)
                        || (parentId != null && parentId.equals(f.getParentId())))
                .peek(f -> f.setChildren(buildTree(all, f.getId())))
                .collect(Collectors.toList());
    }
}
