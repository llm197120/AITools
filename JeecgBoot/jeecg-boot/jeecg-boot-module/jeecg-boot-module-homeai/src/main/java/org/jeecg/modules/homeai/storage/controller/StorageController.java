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
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.TokenUtils;
import org.jeecg.modules.homeai.config.HomeaiFileUrlUtil;
import org.jeecg.modules.homeai.config.HomeaiJwtUtil;
import org.jeecg.modules.homeai.config.HomeaiSecurityUtil;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.jeecg.modules.homeai.storage.entity.StorageFolder;
import org.jeecg.modules.homeai.storage.service.IStorageFileService;
import org.jeecg.modules.homeai.storage.service.IStorageFolderService;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.service.IWxUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    /**
     * 获取文件夹树
     */
    @GetMapping("/folders")
    public Result<?> getFolderTree(HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        List<StorageFolder> tree = folderService.getUserFolderTree(userId, null);
        return Result.OK(tree);
    }

    /**
     * 创建文件夹
     */
    @PostMapping("/folders")
    public Result<?> createFolder(@RequestParam String name,
                                  @RequestParam(required = false) String parentId,
                                  @RequestParam(defaultValue = "private") String visibility,
                                  HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        if (!"private".equals(visibility) && !"family".equals(visibility)) {
            return Result.error("可见性参数无效，仅支持 private/family");
        }
        StorageFolder folder = folderService.createFolder(userId, null, parentId, name, visibility);
        return Result.OK(folder);
    }

    //update-begin---author:admin ---date:2026-07-31  for：修复删除文件夹失效问题（@TableLogic 字段不参与 updateById）-----------
    /**
     * 删除文件夹
     */
    @DeleteMapping("/folders/{id}")
    public Result<?> deleteFolder(@PathVariable String id, HttpServletRequest request) {
        StorageFolder folder = folderService.getById(id);
        if (folder == null) return Result.error("文件夹不存在");
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        if (!userId.equals(folder.getUserId()) && !securityUtil.isConsoleAuthenticated(request)) {
            return Result.error("无权删除该文件夹");
        }
        // @TableLogic 字段不参与 updateById，需通过 update wrapper 显式设置
        folderService.update(new LambdaUpdateWrapper<StorageFolder>()
                .eq(StorageFolder::getId, id)
                .set(StorageFolder::getDelFlag, 1));
        return Result.OK("删除成功");
    }
    //update-end---author:admin ---date:2026-07-31  for：修复删除文件夹失效问题（@TableLogic 字段不参与 updateById）-----------

    //update-begin---author:admin ---date:2026-07-31  for：A3-修改文件夹可见性API-----------
    /**
     * 修改文件夹可见性
     */
    @PatchMapping("/folders/{id}/visibility")
    public Result<?> updateFolderVisibility(@PathVariable String id, @RequestParam String visibility, HttpServletRequest request) {
        StorageFolder folder = folderService.getById(id);
        if (folder == null) return Result.error("文件夹不存在");
        if (!"private".equals(visibility) && !"family".equals(visibility)) {
            return Result.error("可见性参数无效，仅支持 private/family");
        }
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        if (!userId.equals(folder.getUserId()) && !securityUtil.isConsoleAuthenticated(request)) {
            return Result.error("无权修改该文件夹");
        }
        folder.setVisibility(visibility);
        folderService.updateById(folder);
        return Result.OK("可见性修改成功");
    }
    //update-end---author:admin ---date:2026-07-31  for：A3-修改文件夹可见性API-----------

    /**
     * 文件夹内文件列表
     */
    @GetMapping("/folders/{folderId}/files")
    public Result<?> getFiles(@PathVariable String folderId, HttpServletRequest request) {
        StorageFolder folder = folderService.getById(folderId);
        if (folder == null) return Result.error("文件夹不存在");
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        if (!userId.equals(folder.getUserId()) && !securityUtil.isConsoleAuthenticated(request)) {
            return Result.error("无权查看该文件夹");
        }
        List<StorageFile> files = fileService.getFilesByFolder(folderId);
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
                                HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        StorageFile sf = fileService.uploadFile(userId, null, folderId, file, visibility);
        return Result.OK(sf);
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
        if (!userId.equals(sf.getUserId()) && !securityUtil.isConsoleAuthenticated(request)) {
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
        if (!userId.equals(sf.getUserId()) && !securityUtil.isConsoleAuthenticated(request)) {
            return Result.error("无权操作该文件");
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
        List<StorageFile> files = fileService.searchFiles(keyword, userId);
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
        if (!userId.equals(sf.getUserId()) && !securityUtil.isConsoleAuthenticated(request)) {
            return Result.error("无权查看该文件");
        }
        resolveFileUrl(sf);
        return Result.OK(sf);
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
        if (list == null) return;
        for (StorageFile sf : list) {
            resolveFileUrl(sf);
        }
    }

    private void resolveFileUrl(StorageFile sf) {
        if (sf != null && sf.getFileUrl() != null && !sf.getFileUrl().startsWith("http")) {
            sf.setFileUrl(HomeaiFileUrlUtil.toAbsoluteUrl(sf.getFileUrl()));
        }
    }
}
