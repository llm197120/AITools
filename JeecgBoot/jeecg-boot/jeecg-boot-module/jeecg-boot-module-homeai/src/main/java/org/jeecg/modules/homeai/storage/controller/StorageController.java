package org.jeecg.modules.homeai.storage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.homeai.audit.service.IHomeaiAuditLogService;
import com.fasterxml.jackson.databind.JsonNode;
import org.jeecg.modules.homeai.config.HomeaiSecurityUtil;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.config.service.IHomeaiStorageConfigService;
import org.jeecg.modules.homeai.family.entity.Family;
import org.jeecg.modules.homeai.family.service.IFamilyService;
import org.jeecg.modules.homeai.preview.HomeaiFilePreviewDto;
import org.jeecg.modules.homeai.preview.IHomeaiFilePreviewService;
import org.jeecg.modules.homeai.storage.constant.StorageVisibility;
import org.jeecg.modules.homeai.storage.util.StorageFileNameUtil;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.jeecg.modules.homeai.storage.entity.StorageFolder;
import org.jeecg.modules.homeai.storage.service.IStorageFileService;
import org.jeecg.modules.homeai.storage.service.IStorageFolderService;
import org.jeecg.modules.homeai.storage.service.IStorageResourceFamilyService;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.storage.util.StorageAccessUtil;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.service.IWxUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * 资料存储管理
 */
@Slf4j
@RestController
@RequestMapping("/homeai/storage")
public class StorageController {

    @Autowired
    private IStorageFolderService folderService;

    @Autowired
    private IStorageFileService fileService;

    @Autowired
    private IWxUserService wxUserService;

    @Autowired
    private HomeaiSecurityUtil securityUtil;

    @Autowired
    private IHomeaiFileStorageService fileStorageService;

    //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R63】文件预览-----------
    @Autowired
    private IHomeaiFilePreviewService filePreviewService;
    //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R63】文件预览-----------

    @Autowired
    private IStorageResourceFamilyService resourceFamilyService;

    @Autowired
    private IFamilyService familyService;

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R22】资料审计埋点-----------
    @Autowired
    private IHomeaiAuditLogService auditLogService;
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R22】资料审计埋点-----------

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】存储配额配置-----------
    @Autowired
    private IHomeaiStorageConfigService storageConfigService;
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】存储配额配置-----------

    /**
     * 从 Token 解析用户ID：管理端控制台 JWT 或 HomeAI APP JWT（含手机号 userId claim）
     */
    private String getUserId(HttpServletRequest request) {
        //update-begin---author:cursor---date:2026-08-20---for:【Android体验】业务接口统一走 SecurityUtil 解析手机号 JWT-----------
        return securityUtil.getCurrentUserId(request);
        //update-end---author:cursor---date:2026-08-20---for:【Android体验】业务接口统一走 SecurityUtil 解析手机号 JWT-----------
    }

    /** 管理端/具备资料存储权限的角色可查看全部资源 */
    private boolean isStorageAdmin(HttpServletRequest request) {
        if (securityUtil.isConsoleAuthenticated(request)) {
            return true;
        }
        try {
            if (SecurityUtils.getSubject() != null && SecurityUtils.getSubject().isAuthenticated()) {
                return SecurityUtils.getSubject().isPermitted("homeai:storage:file:list");
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void validateVisibility(String visibility) {
        if (!StorageVisibility.isValid(visibility)) {
            throw new JeecgBootException("可见性参数无效，仅支持 private/family/public");
        }
    }

    private List<String> resolveFamilyIds(String visibility, String familyIdsParam, String userFamilyId) {
        if (!StorageVisibility.FAMILY.equals(visibility)) {
            return List.of();
        }
        List<String> ids = new ArrayList<>(StorageAccessUtil.parseFamilyIds(familyIdsParam));
        if (ids.isEmpty() && oConvertUtils.isNotEmpty(userFamilyId)) {
            ids.add(userFamilyId);
        }
        if (ids.isEmpty()) {
            throw new JeecgBootException("家庭可见至少选择一个家庭");
        }
        return ids;
    }

    private List<String> parseFamilyIdsFromBody(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            List<String> ids = new ArrayList<>();
            for (Object item : list) {
                if (item != null && oConvertUtils.isNotEmpty(String.valueOf(item))) {
                    ids.add(String.valueOf(item).trim());
                }
            }
            return ids.stream().distinct().toList();
        }
        return StorageAccessUtil.parseFamilyIds(String.valueOf(raw));
    }

    private String primaryFamilyId(List<String> familyIds) {
        return familyIds == null || familyIds.isEmpty() ? null : familyIds.get(0);
    }

    private void applyFolderVisibility(StorageFolder folder, String visibility, List<String> familyIds) {
        folder.setVisibility(visibility);
        if (StorageVisibility.FAMILY.equals(visibility)) {
            folder.setFamilyId(primaryFamilyId(familyIds));
            resourceFamilyService.replaceFolderFamilies(folder.getId(), familyIds);
        } else {
            folder.setFamilyId(null);
            resourceFamilyService.deleteByFolderId(folder.getId());
        }
    }

    private void applyFileVisibility(StorageFile file, String visibility, List<String> familyIds) {
        file.setVisibility(visibility);
        if (StorageVisibility.FAMILY.equals(visibility)) {
            file.setFamilyId(primaryFamilyId(familyIds));
            resourceFamilyService.replaceFileFamilies(file.getId(), familyIds);
        } else {
            file.setFamilyId(null);
            resourceFamilyService.deleteByFileId(file.getId());
        }
    }

    /** 以 homeai_family_member 为准，wx_user.family_id 仅作兜底 */
    private String resolveUserFamilyId(String userId) {
        if (oConvertUtils.isEmpty(userId)) {
            return null;
        }
        Family family = familyService.getByUserId(userId);
        if (family != null && oConvertUtils.isNotEmpty(family.getId())) {
            return family.getId();
        }
        WxUser user = wxUserService.getById(userId);
        return user != null ? user.getFamilyId() : null;
    }

    /**
     * 获取文件夹树
     */
    @GetMapping("/folders")
    public Result<?> getFolderTree(HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        List<StorageFolder> tree;
        if (isStorageAdmin(request)) {
            tree = folderService.getAllFolderTree();
        } else {
            tree = folderService.getUserFolderTree(userId, resolveUserFamilyId(userId));
        }
        return Result.OK(tree);
    }

    /**
     * 可分配的家庭列表（家庭可见权限时使用）
     */
    @GetMapping("/assignable-families")
    public Result<?> assignableFamilies(HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        List<Map<String, String>> rows = new ArrayList<>();
        if (isStorageAdmin(request)) {
            //update-begin---author:cursor---date:2026-08-20---for:【家庭管理】Family 启用 TableLogic，勿再手动 eq delFlag-----------
            List<Family> families = familyService.list(new LambdaQueryWrapper<Family>()
                    .orderByAsc(Family::getName));
            //update-end---author:cursor---date:2026-08-20---for:【家庭管理】Family 启用 TableLogic，勿再手动 eq delFlag-----------
            for (Family family : families) {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("id", family.getId());
                row.put("name", family.getName());
                rows.add(row);
            }
        } else {
            String familyId = resolveUserFamilyId(userId);
            if (oConvertUtils.isNotEmpty(familyId)) {
                Family family = familyService.getById(familyId);
                if (family != null && (family.getDelFlag() == null || family.getDelFlag() == 0)) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("id", family.getId());
                    row.put("name", family.getName());
                    rows.add(row);
                }
            }
        }
        return Result.OK(rows);
    }

    /**
     * 创建文件夹
     */
    @PostMapping("/folders")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createFolder(@RequestParam String name,
                                  @RequestParam(required = false) String parentId,
                                  @RequestParam(defaultValue = "private") String visibility,
                                  @RequestParam(required = false) String familyIds,
                                  HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        try {
            validateVisibility(visibility);
            String userFamilyId = resolveUserFamilyId(userId);
            List<String> assignedFamilies = resolveFamilyIds(visibility, familyIds, userFamilyId);
            StorageFolder folder = folderService.createFolder(userId, userFamilyId, parentId, name, visibility);
            applyFolderVisibility(folder, visibility, assignedFamilies);
            folder.setUpdateTime(new Date());
            folderService.updateById(folder);
            resourceFamilyService.enrichFolder(folder);
            return Result.OK(folder);
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    //update-begin---author:admin ---date:2026-07-31  for：修复删除文件夹失效问题（@TableLogic 字段不参与 updateById）-----------
    /**
     * 删除文件夹（含其内全部文件及子文件夹）
     */
    @DeleteMapping("/folders/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteFolder(@PathVariable String id, HttpServletRequest request) {
        StorageFolder folder = folderService.getById(id);
        if (folder == null) return Result.error("文件夹不存在");
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        if (!userId.equals(folder.getUserId()) && !isStorageAdmin(request)) {
            return Result.error("无权删除该文件夹");
        }
        folderService.deleteFolderCascade(id);
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】文件夹移入回收站审计-----------
        auditLogService.record(
                userId,
                "storage_folder_move_recycle",
                "storage",
                id,
                "文件夹移入回收站：" + (folder.getName() != null ? folder.getName() : id),
                Collections.singletonMap("folderId", id),
                "success",
                clientIp(request));
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】文件夹移入回收站审计-----------
        return Result.OK("已移入回收站");
    }
    //update-end---author:admin ---date:2026-07-31  for：修复删除文件夹失效问题（@TableLogic 字段不参与 updateById）-----------

    //update-begin---author:admin ---date:2026-07-31  for：A3-修改文件夹可见性API-----------
    /**
     * 修改文件夹可见性
     */
    @PatchMapping("/folders/{id}/visibility")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateFolderVisibility(@PathVariable String id,
                                            @RequestParam String visibility,
                                            @RequestParam(required = false) String familyIds,
                                            HttpServletRequest request) {
        StorageFolder folder = folderService.getById(id);
        if (folder == null) return Result.error("文件夹不存在");
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        if (!userId.equals(folder.getUserId()) && !isStorageAdmin(request)) {
            return Result.error("无权修改该文件夹");
        }
        try {
            validateVisibility(visibility);
            List<String> assignedFamilies = resolveFamilyIds(visibility, familyIds, resolveUserFamilyId(userId));
            applyFolderVisibility(folder, visibility, assignedFamilies);
            folder.setUpdateTime(new Date());
            folderService.updateById(folder);
            resourceFamilyService.enrichFolder(folder);
            return Result.OK("可见性修改成功");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }
    //update-end---author:admin ---date:2026-07-31  for：A3-修改文件夹可见性API-----------

    //update-begin---author:admin ---date:2026-08-05  for：小程序文件夹重命名（无管理端权限注解）-----------
    /**
     * 重命名文件夹（小程序端）
     */
    @PutMapping("/folders/{id}/rename")
    public Result<?> renameFolder(@PathVariable String id, @RequestParam String name, HttpServletRequest request) {
        StorageFolder folder = folderService.getById(id);
        if (folder == null) return Result.error("文件夹不存在");
        if (oConvertUtils.isEmpty(name) || name.trim().isEmpty()) {
            return Result.error("文件夹名称不能为空");
        }
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        if (!userId.equals(folder.getUserId()) && !isStorageAdmin(request)) {
            return Result.error("无权修改该文件夹");
        }
        folder.setName(name.trim());
        folder.setUpdateTime(new Date());
        folderService.updateById(folder);
        return Result.OK("重命名成功");
    }
    //update-end---author:admin ---date:2026-08-05  for：小程序文件夹重命名（无管理端权限注解）-----------

    /**
     * 编辑文件夹（重命名/修改可见性/调整父目录）
     */
    @PutMapping("/folders/{id}")
    @Operation(summary="资料存储-编辑文件夹")
    @RequiresPermissions("homeai:storage:file:list")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateFolder(@PathVariable String id, @RequestBody Map<String, Object> body,
                                  HttpServletRequest request) {
        StorageFolder folder = folderService.getById(id);
        if (folder == null) return Result.error("文件夹不存在");
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        if (!userId.equals(folder.getUserId()) && !isStorageAdmin(request)) {
            return Result.error("无权修改该文件夹");
        }
        if (body.get("name") != null) {
            folder.setName(String.valueOf(body.get("name")));
        }
        if (body.get("visibility") != null) {
            String visibility = String.valueOf(body.get("visibility"));
            try {
                validateVisibility(visibility);
                List<String> assignedFamilies;
                if (body.get("familyIds") != null) {
                    assignedFamilies = parseFamilyIdsFromBody(body.get("familyIds"));
                    if (StorageVisibility.FAMILY.equals(visibility) && assignedFamilies.isEmpty()) {
                        assignedFamilies = resolveFamilyIds(visibility, null, resolveUserFamilyId(userId));
                    }
                } else {
                    assignedFamilies = resolveFamilyIds(visibility, null, resolveUserFamilyId(userId));
                }
                applyFolderVisibility(folder, visibility, assignedFamilies);
            } catch (JeecgBootException e) {
                return Result.error(e.getMessage());
            }
        }
        if (body.get("parentId") != null) {
            String parentId = String.valueOf(body.get("parentId"));
            try {
                folderService.updateFolder(folder, parentId);
            } catch (JeecgBootException e) {
                return Result.error(e.getMessage());
            }
        } else {
            folder.setUpdateTime(new Date());
            folderService.updateById(folder);
        }
        return Result.OK("修改成功");
    }

    //update-begin---author:admin ---date:2026-08-05  for：根目录文件列表与重命名-----------
    /**
     * 根目录文件列表（folderId 为空）
     * 传入 pageNo+pageSize 时返回分页；否则保持全量 List（兼容旧客户端）
     */
    @GetMapping("/files/root")
    public Result<?> getRootFiles(HttpServletRequest request,
                                  @RequestParam(required = false) Integer pageNo,
                                  @RequestParam(required = false) Integer pageSize) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R21】根目录文件分页-----------
        if (pageNo != null && pageSize != null) {
            Page<StorageFile> pageReq = new Page<>(pageNo, pageSize);
            IPage<StorageFile> page = isStorageAdmin(request)
                    ? fileService.pageAllRootFiles(pageReq)
                    : fileService.pageRootFiles(pageReq, userId, resolveUserFamilyId(userId));
            resourceFamilyService.enrichFiles(page.getRecords());
            resolveFileUrls(page.getRecords());
            return Result.OK(page);
        }
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R21】根目录文件分页-----------
        List<StorageFile> files;
        if (isStorageAdmin(request)) {
            files = fileService.getAllRootFiles();
        } else {
            files = fileService.getRootFiles(userId, resolveUserFamilyId(userId));
        }
        resourceFamilyService.enrichFiles(files);
        resolveFileUrls(files);
        return Result.OK(files);
    }

    /**
     * 重命名文件（仅上传者）：只更新 originalName，不修改 storedName / fileUrl，OSS 对象不重命名
     */
    @PutMapping("/files/{id}/rename")
    public Result<?> renameFile(@PathVariable String id, @RequestParam String name, HttpServletRequest request) {
        StorageFile sf = fileService.getById(id);
        if (sf == null) return Result.error("文件不存在");
        if (oConvertUtils.isEmpty(name) || name.trim().isEmpty()) {
            return Result.error("文件名称不能为空");
        }
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        if (!StorageAccessUtil.canWriteFile(userId, sf) && !isStorageAdmin(request)) {
            return Result.error("无权修改该文件");
        }
        String trimmed = StorageFileNameUtil.sanitizeOriginalName(name);
        int dot = trimmed.lastIndexOf('.');
        if (dot > 0 && oConvertUtils.isNotEmpty(sf.getExtension())) {
            String ext = trimmed.substring(dot + 1).toLowerCase();
            if (!sf.getExtension().equalsIgnoreCase(ext)) {
                trimmed = trimmed.substring(0, dot) + "." + sf.getExtension();
            }
        } else if (oConvertUtils.isNotEmpty(sf.getExtension()) && !trimmed.toLowerCase().endsWith("." + sf.getExtension().toLowerCase())) {
            trimmed = trimmed + "." + sf.getExtension();
        }
        sf.setOriginalName(trimmed);
        sf.setUpdateTime(new Date());
        fileService.updateById(sf);
        return Result.OK("重命名成功");
    }
    //update-end---author:admin ---date:2026-08-05  for：根目录文件列表与重命名-----------

    /**
     * 文件夹内文件列表
     * 传入 pageNo+pageSize 时返回分页；否则保持全量 List（兼容旧客户端）
     */
    @GetMapping("/folders/{folderId}/files")
    public Result<?> getFiles(@PathVariable String folderId, HttpServletRequest request,
                              @RequestParam(required = false) Integer pageNo,
                              @RequestParam(required = false) Integer pageSize) {
        StorageFolder folder = folderService.getById(folderId);
        if (folder == null) return Result.error("文件夹不存在");
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        String familyId = resolveUserFamilyId(userId);
        if (!StorageAccessUtil.canAccessFolder(userId, familyId, folder, resourceFamilyService)
                && !isStorageAdmin(request)) {
            return Result.error("无权查看该文件夹");
        }
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R21】文件夹文件分页-----------
        if (pageNo != null && pageSize != null) {
            IPage<StorageFile> page = fileService.pageFilesByFolder(new Page<>(pageNo, pageSize), folderId);
            resourceFamilyService.enrichFiles(page.getRecords());
            resolveFileUrls(page.getRecords());
            return Result.OK(page);
        }
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R21】文件夹文件分页-----------
        List<StorageFile> files = fileService.getFilesByFolder(folderId);
        resourceFamilyService.enrichFiles(files);
        resolveFileUrls(files);
        return Result.OK(files);
    }

    /**
     * 上传文件
     */
    @PostMapping("/files/upload")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> uploadFile(@RequestParam MultipartFile file,
                                @RequestParam(required = false) String folderId,
                                @RequestParam(defaultValue = "private") String visibility,
                                @RequestParam(required = false) String fileName,
                                @RequestParam(required = false) String familyIds,
                                HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        try {
            validateVisibility(visibility);
            String userFamilyId = resolveUserFamilyId(userId);
            StorageFolder folder = null;
            List<String> assignedFamilies = resolveFamilyIds(visibility, familyIds, userFamilyId);
            if (oConvertUtils.isNotEmpty(folderId)) {
                folder = folderService.getById(folderId);
                //update-begin---author:cursor---date:2026-08-20---for:【审查修复】上传到指定文件夹须校验写权限---
                if (folder == null) return Result.error("文件夹不存在");
                if (!StorageAccessUtil.canWriteFolder(userId, folder) && !isStorageAdmin(request)) {
                    return Result.error("无权上传到该文件夹");
                }
                //update-end---author:cursor---date:2026-08-20---for:【审查修复】上传到指定文件夹须校验写权限---
                if (!StorageVisibility.PRIVATE.equals(folder.getVisibility())) {
                    visibility = folder.getVisibility();
                    if (StorageVisibility.FAMILY.equals(visibility)) {
                        assignedFamilies = resourceFamilyService.getFolderFamilyIds(folder.getId());
                        if (assignedFamilies.isEmpty() && oConvertUtils.isNotEmpty(folder.getFamilyId())) {
                            assignedFamilies = List.of(folder.getFamilyId());
                        }
                    } else {
                        assignedFamilies = List.of();
                    }
                }
            }
            StorageFile sf = fileService.uploadFile(userId, userFamilyId, folderId, file, visibility, fileName);
            applyFileVisibility(sf, visibility, assignedFamilies);
            sf.setUpdateTime(new Date());
            fileService.updateById(sf);
            resourceFamilyService.enrichFile(sf);
            fileStorageService.applyAccessUrl(sf);
            return Result.OK(sf);
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改文件可见性
     */
    @PatchMapping("/files/{id}/visibility")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateFileVisibility(@PathVariable String id,
                                          @RequestParam String visibility,
                                          @RequestParam(required = false) String familyIds,
                                          HttpServletRequest request) {
        StorageFile sf = fileService.getById(id);
        if (sf == null) return Result.error("文件不存在");
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        if (!StorageAccessUtil.canWriteFile(userId, sf) && !isStorageAdmin(request)) {
            return Result.error("无权修改该文件");
        }
        try {
            validateVisibility(visibility);
            List<String> assignedFamilies = resolveFamilyIds(visibility, familyIds, resolveUserFamilyId(userId));
            applyFileVisibility(sf, visibility, assignedFamilies);
            sf.setUpdateTime(new Date());
            fileService.updateById(sf);
            resourceFamilyService.enrichFile(sf);
            return Result.OK("可见性修改成功");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除文件（进回收站）
     */
    @DeleteMapping("/files/{id}")
    public Result<?> deleteFile(@PathVariable String id, HttpServletRequest request) {
        StorageFile sf = fileService.getById(id);
        if (sf == null) return Result.error("文件不存在");
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        if (!StorageAccessUtil.canWriteFile(userId, sf) && !isStorageAdmin(request)) {
            return Result.error("无权删除该文件");
        }
        fileService.softDelete(id);
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R22】资料移入回收站审计-----------
        auditLogService.record(
                userId,
                "storage_move_recycle",
                "storage",
                id,
                "移入回收站：" + (sf.getOriginalName() != null ? sf.getOriginalName() : id),
                Collections.singletonMap("fileId", id),
                "success",
                clientIp(request));
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R22】资料移入回收站审计-----------
        return Result.OK("删除成功");
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R22/R23】资料回收站 API（文件+文件夹）-----------
    @GetMapping("/recycleBin")
    @Operation(summary = "资料存储-回收站(管理端)")
    @RequiresPermissions("homeai:storage:file:list")
    public Result<?> recycleBin(@RequestParam(required = false) String keyword,
                                @RequestParam(defaultValue = "file") String type,
                                @RequestParam(defaultValue = "1") int pageNo,
                                @RequestParam(defaultValue = "10") int pageSize) {
        if ("folder".equalsIgnoreCase(type)) {
            IPage<StorageFolder> page = folderService.pageRecycleBin(new Page<>(pageNo, pageSize), keyword);
            resourceFamilyService.enrichFolders(page.getRecords());
            return Result.OK(page);
        }
        IPage<StorageFile> page = fileService.pageRecycleBin(new Page<>(pageNo, pageSize), keyword);
        resourceFamilyService.enrichFiles(page.getRecords());
        resolveFileUrls(page.getRecords());
        return Result.OK(page);
    }

    @PutMapping("/restore")
    @Operation(summary = "资料存储-从回收站恢复(管理端)")
    @RequiresPermissions("homeai:storage:restore")
    public Result<?> restore(@RequestBody JsonNode body, HttpServletRequest request) {
        List<String> fileIds = extractIdList(body, "fileIds");
        List<String> folderIds = extractIdList(body, "folderIds");
        if (body != null && body.isArray()) {
            fileIds = extractIdList(body, null);
        }
        if (fileIds.isEmpty() && folderIds.isEmpty()) {
            return Result.error("请选择要恢复的文件或文件夹");
        }
        if (!folderIds.isEmpty()) {
            folderService.restoreFolders(folderIds);
            auditLogService.record(
                    getUserId(request),
                    "storage_folder_restore",
                    "storage",
                    folderIds.size() == 1 ? folderIds.get(0) : null,
                    "恢复文件夹 " + folderIds.size() + " 个",
                    Collections.singletonMap("folderIds", folderIds),
                    "success",
                    clientIp(request));
        }
        if (!fileIds.isEmpty()) {
            fileService.restoreFiles(fileIds);
            auditLogService.record(
                    getUserId(request),
                    "storage_restore",
                    "storage",
                    fileIds.size() == 1 ? fileIds.get(0) : null,
                    "恢复文件 " + fileIds.size() + " 个",
                    Collections.singletonMap("ids", fileIds),
                    "success",
                    clientIp(request));
        }
        return Result.OK("恢复成功");
    }

    @DeleteMapping("/deletePermanently")
    @Operation(summary = "资料存储-彻底删除(管理端)")
    @RequiresPermissions("homeai:storage:deletePermanently")
    public Result<?> deletePermanently(@RequestBody JsonNode body, HttpServletRequest request) {
        List<String> fileIds = extractIdList(body, "fileIds");
        List<String> folderIds = extractIdList(body, "folderIds");
        if (body != null && body.isArray()) {
            fileIds = extractIdList(body, null);
        }
        if (fileIds.isEmpty() && folderIds.isEmpty()) {
            return Result.error("请选择要彻底删除的文件或文件夹");
        }
        if (!folderIds.isEmpty()) {
            folderService.deleteFoldersPermanently(folderIds);
            auditLogService.record(
                    getUserId(request),
                    "storage_folder_delete_permanently",
                    "storage",
                    folderIds.size() == 1 ? folderIds.get(0) : null,
                    "彻底删除文件夹 " + folderIds.size() + " 个",
                    Collections.singletonMap("folderIds", folderIds),
                    "success",
                    clientIp(request));
        }
        if (!fileIds.isEmpty()) {
            fileService.deletePermanently(fileIds);
            auditLogService.record(
                    getUserId(request),
                    "storage_delete_permanently",
                    "storage",
                    fileIds.size() == 1 ? fileIds.get(0) : null,
                    "彻底删除文件 " + fileIds.size() + " 个",
                    Collections.singletonMap("ids", fileIds),
                    "success",
                    clientIp(request));
        }
        return Result.OK("彻底删除成功");
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R24】小程序用户侧回收站-----------
    @GetMapping("/my/recycleBin")
    @Operation(summary = "资料存储-我的回收站")
    public Result<?> myRecycleBin(@RequestParam(required = false) String keyword,
                                  @RequestParam(defaultValue = "file") String type,
                                  @RequestParam(defaultValue = "1") int pageNo,
                                  @RequestParam(defaultValue = "20") int pageSize,
                                  HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) {
            return Result.error("未登录");
        }
        if ("folder".equalsIgnoreCase(type)) {
            IPage<StorageFolder> page = folderService.pageMyRecycleBin(new Page<>(pageNo, pageSize), userId, keyword);
            resourceFamilyService.enrichFolders(page.getRecords());
            return Result.OK(page);
        }
        IPage<StorageFile> page = fileService.pageMyRecycleBin(new Page<>(pageNo, pageSize), userId, keyword);
        resourceFamilyService.enrichFiles(page.getRecords());
        resolveFileUrls(page.getRecords());
        return Result.OK(page);
    }

    @PutMapping("/my/restore")
    @Operation(summary = "资料存储-恢复我的回收站项")
    public Result<?> myRestore(@RequestBody JsonNode body, HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) {
            return Result.error("未登录");
        }
        List<String> fileIds = filterOwnedFileIds(extractIdList(body, "fileIds"), userId);
        List<String> folderIds = filterOwnedFolderIds(extractIdList(body, "folderIds"), userId);
        if (body != null && body.isArray()) {
            fileIds = filterOwnedFileIds(extractIdList(body, null), userId);
        }
        if (fileIds.isEmpty() && folderIds.isEmpty()) {
            return Result.error("无可恢复的项目（仅能恢复自己删除的）");
        }
        if (!folderIds.isEmpty()) {
            folderService.restoreFolders(folderIds);
        }
        if (!fileIds.isEmpty()) {
            fileService.restoreFiles(fileIds);
        }
        return Result.OK("恢复成功");
    }

    @DeleteMapping("/my/deletePermanently")
    @Operation(summary = "资料存储-彻底删除我的回收站项")
    public Result<?> myDeletePermanently(@RequestBody JsonNode body, HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) {
            return Result.error("未登录");
        }
        List<String> fileIds = filterOwnedFileIds(extractIdList(body, "fileIds"), userId);
        List<String> folderIds = filterOwnedFolderIds(extractIdList(body, "folderIds"), userId);
        if (body != null && body.isArray()) {
            fileIds = filterOwnedFileIds(extractIdList(body, null), userId);
        }
        if (fileIds.isEmpty() && folderIds.isEmpty()) {
            return Result.error("无可删除的项目（仅能删除自己的）");
        }
        if (!folderIds.isEmpty()) {
            folderService.deleteFoldersPermanently(folderIds);
        }
        if (!fileIds.isEmpty()) {
            fileService.deletePermanently(fileIds);
        }
        return Result.OK("彻底删除成功");
    }

    private List<String> filterOwnedFileIds(List<String> ids, String userId) {
        List<String> owned = new ArrayList<>();
        if (ids == null) {
            return owned;
        }
        for (String id : ids) {
            StorageFile sf = fileService.getOne(
                    new LambdaQueryWrapper<StorageFile>().eq(StorageFile::getId, id).last("LIMIT 1"), false);
            if (sf != null && userId.equals(sf.getUserId()) && Integer.valueOf(1).equals(sf.getDelFlag())) {
                owned.add(id);
            }
        }
        return owned;
    }

    private List<String> filterOwnedFolderIds(List<String> ids, String userId) {
        List<String> owned = new ArrayList<>();
        if (ids == null) {
            return owned;
        }
        for (String id : ids) {
            StorageFolder folder = folderService.getById(id);
            if (folder != null && userId.equals(folder.getUserId()) && Integer.valueOf(1).equals(folder.getDelFlag())) {
                owned.add(id);
            }
        }
        return owned;
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R24】小程序用户侧回收站-----------

    private List<String> extractIdList(JsonNode body, String field) {
        List<String> ids = new ArrayList<>();
        if (body == null || body.isNull()) {
            return ids;
        }
        JsonNode arr = field == null ? body : body.get(field);
        if (arr == null || !arr.isArray()) {
            return ids;
        }
        for (JsonNode n : arr) {
            if (n != null && !n.isNull() && oConvertUtils.isNotEmpty(n.asText())) {
                ids.add(n.asText());
            }
        }
        return ids;
    }

    private String clientIp(HttpServletRequest r) {
        if (r == null) {
            return null;
        }
        String ip = r.getHeader("X-Forwarded-For");
        if (oConvertUtils.isNotEmpty(ip)) {
            //update-begin---author:cursor ---date:2026-08-13 for：【安全加固】XFF 取最右侧由可信代理追加的真实 IP（左侧可被客户端伪造），并校验格式-----------
            String[] parts = ip.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = parts[i].trim();
                if (isPlausibleIp(candidate)) {
                    return candidate;
                }
            }
            //update-end---author:cursor ---date:2026-08-13 for：【安全加固】XFF 信任修复-----------
        }
        return r.getRemoteAddr();
    }

    /** 粗校验 IP 格式（IPv4 或 IPv6），过滤 XFF 注入的非 IP 内容 */
    private static boolean isPlausibleIp(String ip) {
        if (oConvertUtils.isEmpty(ip)) {
            return false;
        }
        return ip.matches("^[0-9]{1,3}(\\.[0-9]{1,3}){3}$") || ip.contains(":");
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R22/R23】资料回收站 API（文件+文件夹）-----------

    /**
     * 收藏/取消收藏
     */
    @PutMapping("/files/{id}/favorite")
    public Result<?> toggleFavorite(@PathVariable String id, HttpServletRequest request) {
        StorageFile sf = fileService.getById(id);
        if (sf == null) return Result.error("文件不存在");
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        StorageFolder folder = oConvertUtils.isNotEmpty(sf.getFolderId())
                ? folderService.getById(sf.getFolderId()) : null;
        String familyId = resolveUserFamilyId(userId);
        if (!StorageAccessUtil.canAccessFile(userId, familyId, sf, folder, resourceFamilyService)
                && !isStorageAdmin(request)) {
            return Result.error("无权操作该文件");
        }
        if (!StorageAccessUtil.canWriteFile(userId, sf) && !isStorageAdmin(request)) {
            return Result.error("仅上传者可收藏自己的文件");
        }
        fileService.toggleFavorite(id);
        return Result.OK("操作成功");
    }

    /**
     * 搜索文件
     */
    @GetMapping("/files/search")
    public Result<?> searchFiles(@RequestParam String keyword, HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        List<StorageFile> files;
        if (isStorageAdmin(request)) {
            files = fileService.searchAllFiles(keyword);
        } else {
            files = fileService.searchFiles(keyword, userId, resolveUserFamilyId(userId));
        }
        resourceFamilyService.enrichFiles(files);
        resolveFileUrls(files);
        return Result.OK(files);
    }

    /**
     * 文件详情
     */
    @GetMapping("/files/{id}")
    public Result<?> getFileDetail(@PathVariable String id, HttpServletRequest request) {
        StorageFile sf = fileService.getById(id);
        if (sf == null) return Result.error("文件不存在");
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        StorageFolder folder = oConvertUtils.isNotEmpty(sf.getFolderId())
                ? folderService.getById(sf.getFolderId()) : null;
        String familyId = resolveUserFamilyId(userId);
        if (!StorageAccessUtil.canAccessFile(userId, familyId, sf, folder, resourceFamilyService)
                && !isStorageAdmin(request)) {
            return Result.error("无权查看该文件");
        }
        resourceFamilyService.enrichFile(sf);
        resolveFileUrl(sf);
        return Result.OK(sf);
    }

    /**
     * 刷新文件预签名访问 URL（私有 OSS）
     */
    @GetMapping("/files/{id}/access-url")
    public Result<?> getFileAccessUrl(@PathVariable String id, HttpServletRequest request) {
        StorageFile sf = fileService.getById(id);
        if (sf == null) return Result.error("文件不存在");
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        StorageFolder folder = oConvertUtils.isNotEmpty(sf.getFolderId())
                ? folderService.getById(sf.getFolderId()) : null;
        String familyId = resolveUserFamilyId(userId);
        if (!StorageAccessUtil.canAccessFile(userId, familyId, sf, folder, resourceFamilyService)
                && !isStorageAdmin(request)) {
            return Result.error("无权查看该文件");
        }
        return Result.OK(fileStorageService.resolveAccessUrl(sf.getFileUrl()));
    }

    //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R63】文件预览-----------
    @GetMapping("/files/{id}/preview")
    @Operation(summary = "资料文件-预览描述")
    public Result<?> previewFile(@PathVariable String id, HttpServletRequest request) {
        StorageFile sf = fileService.getById(id);
        Result<?> denied = assertCanReadFile(sf, request);
        if (denied != null) return denied;
        return Result.OK(filePreviewService.previewStorage(sf));
    }

    @PostMapping("/files/{id}/preview-pdf")
    @Operation(summary = "资料文件-Office 转 PDF 预览")
    public Result<?> previewFilePdf(@PathVariable String id, HttpServletRequest request) {
        StorageFile sf = fileService.getById(id);
        Result<?> denied = assertCanReadFile(sf, request);
        if (denied != null) return denied;
        String userId = getUserId(request);
        try {
            HomeaiFilePreviewDto dto = filePreviewService.ensureStoragePreviewPdf(userId, sf);
            return Result.OK(dto);
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    private Result<?> assertCanReadFile(StorageFile sf, HttpServletRequest request) {
        if (sf == null) return Result.error("文件不存在");
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        StorageFolder folder = oConvertUtils.isNotEmpty(sf.getFolderId())
                ? folderService.getById(sf.getFolderId()) : null;
        String familyId = resolveUserFamilyId(userId);
        if (!StorageAccessUtil.canAccessFile(userId, familyId, sf, folder, resourceFamilyService)
                && !isStorageAdmin(request)) {
            return Result.error("无权查看该文件");
        }
        return null;
    }
    //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R63】文件预览-----------

    /**
     * 文件夹列表（管理端分页）
     */
    @GetMapping("/folder-list")
    @Operation(summary="资料存储-文件夹列表(管理端)")
    @RequiresPermissions("homeai:storage:file:list")
    public Result<?> listFolders(StorageFolder folder,
                                 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                 HttpServletRequest req) {
        QueryWrapper<StorageFolder> queryWrapper = QueryGenerator.initQueryWrapper(folder, req.getParameterMap());
        Page<StorageFolder> page = new Page<>(pageNo, pageSize);
        IPage<StorageFolder> pageList = folderService.page(page, queryWrapper);
        resourceFamilyService.enrichFolders(pageList.getRecords());
        return Result.OK(pageList);
    }

    /**
     * 文件列表（管理端分页）
     */
    @GetMapping("/file-list")
    @Operation(summary="资料存储-文件列表(管理端)")
    @RequiresPermissions("homeai:storage:file:list")
    public Result<?> listFiles(StorageFile file,
                               @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                               HttpServletRequest req) {
        QueryWrapper<StorageFile> queryWrapper = QueryGenerator.initQueryWrapper(file, req.getParameterMap());
        Page<StorageFile> page = new Page<>(pageNo, pageSize);
        IPage<StorageFile> pageList = fileService.page(page, queryWrapper);
        resourceFamilyService.enrichFiles(pageList.getRecords());
        resolveFileUrls(pageList.getRecords());
        return Result.OK(pageList);
    }

    /**
     * 空间使用统计（管理端）
     */
    @GetMapping("/stats")
    @Operation(summary="资料存储-空间统计(管理端)")
    @RequiresPermissions("homeai:storage:file:list")
    public Result<?> stats() {
        List<StorageFile> files = fileService.list(
                new LambdaQueryWrapper<StorageFile>().eq(StorageFile::getDelFlag, 0));
        long totalSize = 0;
        java.util.Map<String, Long> sizeByUser = new java.util.LinkedHashMap<>();
        java.util.Map<String, Long> countByUser = new java.util.LinkedHashMap<>();
        for (StorageFile f : files) {
            long size = f.getFileSize() != null ? f.getFileSize() : 0;
            totalSize += size;
            if (f.getUserId() != null) {
                sizeByUser.merge(f.getUserId(), size, Long::sum);
                countByUser.merge(f.getUserId(), 1L, Long::sum);
            }
        }
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】统计附带配额-----------
        long limitBytes = storageConfigService.getDefaultUserLimitBytes();
        int warnPercent = storageConfigService.getWarnPercent();
        java.util.List<java.util.Map<String, Object>> perUser = new java.util.ArrayList<>();
        for (String uid : sizeByUser.keySet()) {
            long used = sizeByUser.get(uid);
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("userId", uid);
            row.put("fileCount", countByUser.getOrDefault(uid, 0L));
            row.put("totalSize", used);
            row.put("limitBytes", limitBytes);
            row.put("usedPercent", limitBytes > 0 ? Math.min(100, used * 100.0 / limitBytes) : 0);
            row.put("overWarn", limitBytes > 0 && used * 100.0 / limitBytes >= warnPercent);
            perUser.add(row);
        }
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭维度统计-----------
        long familyLimitBytes = storageConfigService.getDefaultFamilyLimitBytes();
        java.util.List<java.util.Map<String, Object>> perFamily = fileService.listFamilyQuotaBoard(null, null, null);
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭维度统计-----------
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("totalFiles", files.size());
        result.put("totalSize", totalSize);
        result.put("defaultUserLimitBytes", limitBytes);
        result.put("defaultFamilyLimitBytes", familyLimitBytes);
        result.put("warnPercent", warnPercent);
        result.put("perUser", perUser);
        result.put("perFamily", perFamily);
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】统计附带配额-----------
        return Result.OK(result);
    }

    /**
     * 兼容历史相对地址数据：统一转换为绝对访问地址
     */
    private void resolveFileUrls(List<StorageFile> list) {
        fileStorageService.applyAccessUrls(list);
    }

    private void resolveFileUrl(StorageFile sf) {
        fileStorageService.applyAccessUrl(sf);
    }
}
