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
import org.jeecg.common.api.CommonAPI;
import org.jeecg.common.api.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.TokenUtils;
import org.jeecg.modules.homeai.config.HomeaiJwtUtil;
import org.jeecg.modules.homeai.config.HomeaiSecurityUtil;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.family.entity.Family;
import org.jeecg.modules.homeai.family.service.IFamilyService;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @Lazy
    @Autowired
    private CommonAPI commonApi;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private HomeaiSecurityUtil securityUtil;

    @Autowired
    private IHomeaiFileStorageService fileStorageService;

    @Autowired
    private IStorageResourceFamilyService resourceFamilyService;

    @Autowired
    private IFamilyService familyService;

    /**
     * 从 Token 解析用户ID
     * 支持三种认证来源：
     *  1. Shiro 认证（管理端，若 /homeai/** 被放行为 anon 则不生效）
     *  2. JeecgBoot 标准 JWT（管理端，X-Access-Token 头）
     *  3. HomeaiJWT（小程序端，X-Access-Token 头）
     */
    private String getUserId(HttpServletRequest request) {
        // 1. 优先从 Shiro 认证获取（管理端已通过 Shiro 过滤器的情况）
        try {
            if (SecurityUtils.getSubject() != null && SecurityUtils.getSubject().isAuthenticated()) {
                Object principal = SecurityUtils.getSubject().getPrincipal();
                if (principal instanceof LoginUser) {
                    return ((LoginUser) principal).getId();
                }
                return principal != null ? principal.toString() : null;
            }
        } catch (Exception ignored) {}

        String token = request.getHeader("X-Access-Token");
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        // 2. 尝试解析 JeecgBoot 标准 JWT（管理端）
        try {
            String username = JwtUtil.getUsername(token);
            if (username != null && TokenUtils.verifyToken(token, commonApi, redisUtil)) {
                LoginUser loginUser = TokenUtils.getLoginUser(username, commonApi, redisUtil);
                if (loginUser != null) {
                    return loginUser.getId();
                }
            }
        } catch (Exception ignored) {}

        // 3. 回退到 HomeaiJWT 认证（小程序端）
        String openid = HomeaiJwtUtil.getOpenid(token);
        if (openid == null) return null;
        WxUser user = wxUserService.getByOpenid(openid);
        return user != null ? user.getId() : null;
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
            List<Family> families = familyService.list(new LambdaQueryWrapper<Family>()
                    .eq(Family::getDelFlag, 0)
                    .orderByAsc(Family::getName));
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
    public Result<?> deleteFolder(@PathVariable String id, HttpServletRequest request) {
        StorageFolder folder = folderService.getById(id);
        if (folder == null) return Result.error("文件夹不存在");
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        if (!userId.equals(folder.getUserId()) && !isStorageAdmin(request)) {
            return Result.error("无权删除该文件夹");
        }
        folderService.deleteFolderCascade(id);
        return Result.OK("删除成功");
    }
    //update-end---author:admin ---date:2026-07-31  for：修复删除文件夹失效问题（@TableLogic 字段不参与 updateById）-----------

    //update-begin---author:admin ---date:2026-07-31  for：A3-修改文件夹可见性API-----------
    /**
     * 修改文件夹可见性
     */
    @PatchMapping("/folders/{id}/visibility")
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
     */
    @GetMapping("/files/root")
    public Result<?> getRootFiles(HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
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
     */
    @GetMapping("/folders/{folderId}/files")
    public Result<?> getFiles(@PathVariable String folderId, HttpServletRequest request) {
        StorageFolder folder = folderService.getById(folderId);
        if (folder == null) return Result.error("文件夹不存在");
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        String familyId = resolveUserFamilyId(userId);
        if (!StorageAccessUtil.canAccessFolder(userId, familyId, folder, resourceFamilyService)
                && !isStorageAdmin(request)) {
            return Result.error("无权查看该文件夹");
        }
        List<StorageFile> files = fileService.getFilesByFolder(folderId);
        resourceFamilyService.enrichFiles(files);
        resolveFileUrls(files);
        return Result.OK(files);
    }

    /**
     * 上传文件
     */
    @PostMapping("/files/upload")
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
            if (folderId != null) {
                folder = folderService.getById(folderId);
                if (folder != null && !StorageVisibility.PRIVATE.equals(folder.getVisibility())) {
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
     * 删除文件
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
        return Result.OK("删除成功");
    }

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
        java.util.List<java.util.Map<String, Object>> perUser = new java.util.ArrayList<>();
        for (String uid : sizeByUser.keySet()) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("userId", uid);
            row.put("fileCount", countByUser.getOrDefault(uid, 0L));
            row.put("totalSize", sizeByUser.get(uid));
            perUser.add(row);
        }
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("totalFiles", files.size());
        result.put("totalSize", totalSize);
        result.put("perUser", perUser);
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
