package org.jeecg.modules.homeai.storage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.homeai.storage.constant.StorageVisibility;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.jeecg.modules.homeai.storage.entity.StorageFolder;
import org.jeecg.modules.homeai.storage.mapper.StorageFileMapper;
import org.jeecg.modules.homeai.storage.mapper.StorageFolderMapper;
import org.jeecg.modules.homeai.storage.service.IStorageFileService;
import org.jeecg.modules.homeai.storage.service.IStorageFolderService;
import org.jeecg.modules.homeai.storage.service.IStorageResourceFamilyService;
import org.jeecg.modules.homeai.storage.util.StorageVisibilityQueryUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StorageFolderServiceImpl extends ServiceImpl<StorageFolderMapper, StorageFolder>
        implements IStorageFolderService {

    @org.springframework.beans.factory.annotation.Autowired
    private StorageFileMapper fileMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private IStorageFileService fileService;

    @org.springframework.beans.factory.annotation.Autowired
    private IStorageResourceFamilyService resourceFamilyService;

    /** 设计文档：文件夹最多 5 级嵌套（level 0~4） */
    private static final int MAX_FOLDER_DEPTH = 5;

    @Override
    public List<StorageFolder> getUserFolderTree(String userId, String familyId) {
        LambdaQueryWrapper<StorageFolder> query = new LambdaQueryWrapper<>();
        query.eq(StorageFolder::getDelFlag, 0);
        StorageVisibilityQueryUtil.applyReadableFolderFilter(query, userId, familyId);
        return buildFolderTreeWithCounts(list(query));
    }

    @Override
    public List<StorageFolder> getAllFolderTree() {
        LambdaQueryWrapper<StorageFolder> query = new LambdaQueryWrapper<>();
        query.eq(StorageFolder::getDelFlag, 0);
        return buildFolderTreeWithCounts(list(query));
    }

    private List<StorageFolder> buildFolderTreeWithCounts(List<StorageFolder> allFolders) {
        //update-begin---author:cursor ---date:2026-08-13 for：【性能优化】文件数由逐文件夹 selectCount(N+1) 改为单条 GROUP BY 批量查询-----------
        if (allFolders != null && !allFolders.isEmpty()) {
            List<String> folderIds = allFolders.stream()
                    .map(StorageFolder::getId)
                    .collect(Collectors.toList());
            Map<String, Integer> countMap = new HashMap<>();
            List<Map<String, Object>> rows = fileMapper.selectMaps(new QueryWrapper<StorageFile>()
                    .select("folder_id AS fid, COUNT(*) AS cnt")
                    .in("folder_id", folderIds)
                    .eq("del_flag", 0)
                    .groupBy("folder_id"));
            for (Map<String, Object> row : rows) {
                Object fid = extractMapValue(row, "fid");
                Object cnt = extractMapValue(row, "cnt");
                if (fid != null) {
                    countMap.put(fid.toString(), cnt instanceof Number ? ((Number) cnt).intValue() : 0);
                }
            }
            for (StorageFolder folder : allFolders) {
                folder.setFileCount(countMap.getOrDefault(folder.getId(), 0));
            }
        }
        //update-end---author:cursor ---date:2026-08-13 for：【性能优化】文件夹文件数批量统计-----------
        resourceFamilyService.enrichFolders(allFolders);
        return buildTree(allFolders, null);
    }

    /** 大小写不敏感地从聚合行取值（兼容不同数据库别名返回大小写） */
    private static Object extractMapValue(Map<String, Object> row, String key) {
        if (row == null) {
            return null;
        }
        if (row.containsKey(key)) {
            return row.get(key);
        }
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        return null;
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
        //update-begin---author:admin ---date:2026-08-04  for：文件夹上下级循环引用校验-----------
        validateParentNotCycle(null, parentId);
        validateFolderDepth(parentId);
        if (parentId != null && !parentId.isEmpty()) {
            StorageFolder parent = getById(parentId);
            if (parent == null || Objects.equals(parent.getDelFlag(), 1)) {
                throw new JeecgBootException("上级文件夹不存在");
            }
            if (!userId.equals(parent.getUserId())) {
                throw new JeecgBootException("无权在上级文件夹下创建");
            }
        }
        //update-end---author:admin ---date:2026-08-04  for：文件夹上下级循环引用校验-----------
        StorageFolder folder = new StorageFolder();
        folder.setUserId(userId);
        folder.setFamilyId(familyId);
        folder.setParentId(parentId);
        folder.setName(name);
        folder.setVisibility(visibility != null ? visibility : StorageVisibility.PRIVATE);
        folder.setLevel(parentId != null ? getParentLevel(parentId) + 1 : 0);
        folder.setCreateTime(new Date());
        folder.setUpdateTime(new Date());
        save(folder);
        return folder;
    }

    @Override
    public void updateFolder(StorageFolder folder, String newParentId) {
        String normalizedParentId = (newParentId == null || newParentId.isEmpty()) ? null : newParentId;
        validateParentNotCycle(folder.getId(), normalizedParentId);
        validateFolderDepth(normalizedParentId);
        if (normalizedParentId != null) {
            StorageFolder parent = getById(normalizedParentId);
            if (parent == null || Objects.equals(parent.getDelFlag(), 1)) {
                throw new JeecgBootException("上级文件夹不存在");
            }
            if (!folder.getUserId().equals(parent.getUserId())) {
                throw new JeecgBootException("无权移动到该上级文件夹");
            }
            folder.setParentId(normalizedParentId);
            folder.setLevel(getParentLevel(normalizedParentId) + 1);
        } else {
            folder.setParentId(null);
            folder.setLevel(0);
        }
        folder.setUpdateTime(new Date());
        updateById(folder);
        refreshChildLevels(folder.getId(), folder.getLevel());
    }

    @Override
    public void validateParentNotCycle(String folderId, String parentId) {
        if (parentId == null || parentId.isEmpty()) {
            return;
        }
        if (folderId != null && parentId.equals(folderId)) {
            throw new JeecgBootException("不能将文件夹设为自身的上级");
        }
        String current = parentId;
        int depth = 0;
        while (current != null && depth < 100) {
            if (folderId != null && current.equals(folderId)) {
                throw new JeecgBootException("不能将文件夹移动到其子文件夹下，会造成循环引用");
            }
            StorageFolder parent = getById(current);
            if (parent == null || Objects.equals(parent.getDelFlag(), 1)) {
                break;
            }
            current = parent.getParentId();
            depth++;
        }
    }

    private void validateFolderDepth(String parentId) {
        int newLevel = parentId == null || parentId.isEmpty() ? 0 : getParentLevel(parentId) + 1;
        if (newLevel >= MAX_FOLDER_DEPTH) {
            throw new JeecgBootException("文件夹层级不能超过 " + MAX_FOLDER_DEPTH + " 级");
        }
    }

    private void refreshChildLevels(String parentId, int parentLevel) {
        List<StorageFolder> children = getChildFolders(parentId);
        for (StorageFolder child : children) {
            child.setLevel(parentLevel + 1);
            child.setUpdateTime(new Date());
            updateById(child);
            refreshChildLevels(child.getId(), child.getLevel());
        }
    }

    @Override
    public List<StorageFolder> getChildFolders(String parentId) {
        LambdaQueryWrapper<StorageFolder> query = new LambdaQueryWrapper<>();
        query.eq(StorageFolder::getParentId, parentId)
                .eq(StorageFolder::getDelFlag, 0)
                .orderByAsc(StorageFolder::getCreateTime);
        return list(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolderCascade(String folderId) {
        if (oConvertUtils.isEmpty(folderId)) {
            return;
        }
        List<StorageFolder> children = getChildFolders(folderId);
        for (StorageFolder child : children) {
            deleteFolderCascade(child.getId());
        }
        fileService.softDeleteByFolderId(folderId);
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】文件夹软删保留家庭关联与 deleted_at-----------
        // 家庭关联保留至彻底删除，便于回收站恢复后可见性不丢
        Date now = new Date();
        update(new LambdaUpdateWrapper<StorageFolder>()
                .eq(StorageFolder::getId, folderId)
                .set(StorageFolder::getDelFlag, 1)
                .set(StorageFolder::getDeletedAt, now));
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】文件夹软删保留家庭关联与 deleted_at-----------
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】文件夹回收站-----------
    @Override
    public IPage<StorageFolder> pageRecycleBin(Page<StorageFolder> page, String keyword) {
        return baseMapper.selectRecycleBinPage(page, keyword);
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R24】用户侧文件夹回收站-----------
    @Override
    public IPage<StorageFolder> pageMyRecycleBin(Page<StorageFolder> page, String userId, String keyword) {
        return baseMapper.selectMyRecycleBinPage(page, userId, keyword);
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R24】用户侧文件夹回收站-----------

    @Override
    public void restoreFolders(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Set<String> folderIds = new LinkedHashSet<>();
        for (String id : ids) {
            if (oConvertUtils.isEmpty(id)) {
                continue;
            }
            collectDeletedSubtree(id, folderIds);
        }
        // 父级若仍删除则一并恢复，避免恢复到“已删父级”
        Set<String> withAncestors = new LinkedHashSet<>(folderIds);
        for (String id : folderIds) {
            collectDeletedAncestors(id, withAncestors);
        }
        for (String id : withAncestors) {
            baseMapper.restoreById(id);
        }
        List<StorageFile> files = fileMapper.selectList(new LambdaQueryWrapper<StorageFile>()
                .in(StorageFile::getFolderId, withAncestors)
                .eq(StorageFile::getDelFlag, 1));
        if (!files.isEmpty()) {
            List<String> fileIds = files.stream().map(StorageFile::getId).collect(Collectors.toList());
            fileService.restoreFiles(fileIds);
        }
    }

    @Override
    public void deleteFoldersPermanently(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Set<String> folderIds = new LinkedHashSet<>();
        for (String id : ids) {
            if (oConvertUtils.isEmpty(id)) {
                continue;
            }
            collectDeletedSubtree(id, folderIds);
        }
        if (folderIds.isEmpty()) {
            return;
        }
        List<StorageFile> files = fileMapper.selectList(new LambdaQueryWrapper<StorageFile>()
                .in(StorageFile::getFolderId, folderIds)
                .eq(StorageFile::getDelFlag, 1));
        if (!files.isEmpty()) {
            fileService.deletePermanently(files.stream().map(StorageFile::getId).collect(Collectors.toList()));
        }
        for (String folderId : folderIds) {
            resourceFamilyService.deleteByFolderId(folderId);
        }
        baseMapper.deletePermanentlyByIds(folderIds);
    }

    private void collectDeletedSubtree(String folderId, Set<String> out) {
        if (!out.add(folderId)) {
            return;
        }
        List<StorageFolder> children = baseMapper.selectDeletedChildren(folderId);
        if (children == null) {
            return;
        }
        for (StorageFolder child : children) {
            collectDeletedSubtree(child.getId(), out);
        }
    }

    private void collectDeletedAncestors(String folderId, Set<String> out) {
        StorageFolder cur = getById(folderId);
        Set<String> guard = new HashSet<>();
        while (cur != null && oConvertUtils.isNotEmpty(cur.getParentId()) && guard.add(cur.getParentId())) {
            StorageFolder parent = getById(cur.getParentId());
            if (parent == null || parent.getDelFlag() == null || parent.getDelFlag() != 1) {
                break;
            }
            out.add(parent.getId());
            cur = parent;
        }
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】文件夹回收站-----------

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
