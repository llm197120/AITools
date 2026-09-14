package org.jeecg.modules.homeai.appversion.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.appversion.entity.HomeaiAppVersion;
import org.jeecg.modules.homeai.appversion.service.IHomeaiAppVersionService;
import org.jeecg.modules.homeai.appversion.service.HomeaiAppPackageCache;
import org.jeecg.modules.homeai.appversion.util.HomeaiAppPackageDownloadPaths;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.preview.HomeaiFileMime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/homeai/app/version")
public class HomeaiAppVersionController {

    @Autowired
    private IHomeaiAppVersionService appVersionService;

    @Autowired
    private HomeaiAppPackageCache appPackageCache;

    @Autowired
    private IHomeaiFileStorageService fileStorageService;

    @GetMapping
    @Operation(summary = "APP当前版本（公开，启动页探测）")
    public Result<?> publicCurrent() {
        return Result.OK(appVersionService.toPublic(appVersionService.requireCurrent()));
    }

    //update-begin---author:cursor---date:2026-09-04---for:【APP热更新】zip 与 APK 共用代理下载（kind=resource）---
    /**
     * APK / H5 zip 下载（匿名公开）：OSS 预签名直链对部分类型不可用，由后端 SDK 拉流后转发。
     * kind=resource 为热更新 zip，缺省或 apk 为安装包。
     */
    @GetMapping("/package/download")
    public void downloadPackage(@RequestParam(value = "kind", required = false) String kind,
                                HttpServletResponse response) {
        HomeaiAppVersion row = appVersionService.requireCurrent();
        boolean resource = HomeaiAppPackageDownloadPaths.isResourceKind(kind);
        String stored = resource ? row.getResourceUrl() : row.getApkUrl();
        if (oConvertUtils.isEmpty(stored)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String ext = resource ? "zip" : "apk";
        String downloadName = resource
                ? "homeai-h5-" + row.getVersionCode() + ".zip"
                : "homeai-" + row.getVersionCode() + ".apk";
        try {
            Path cached = resource ? appPackageCache.lookupResource(row) : appPackageCache.lookupApk(row);
            if (HomeaiAppPackageCache.isUsable(cached)) {
                HomeaiFileMime.writeLocalFile(response, cached, downloadName, ext);
                return;
            }
            Path dest = resource ? appPackageCache.targetResource(row) : appPackageCache.targetApk(row);
            synchronized (appPackageCache.lockFor(dest)) {
                cached = resource ? appPackageCache.lookupResource(row) : appPackageCache.lookupApk(row);
                if (HomeaiAppPackageCache.isUsable(cached)) {
                    HomeaiFileMime.writeLocalFile(response, cached, downloadName, ext);
                    return;
                }
                fileStorageService.writeToResponse(stored, response, downloadName, ext, dest);
                if (HomeaiAppPackageCache.isUsable(dest)) {
                    appPackageCache.pruneOthers(dest);
                }
            }
        } catch (Exception e) {
            log.error(resource ? "热更新包下载失败" : "APK 下载失败", e);
            if (!response.isCommitted()) {
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "更新包读取失败");
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
    }
    //update-end---author:cursor---date:2026-09-04---for:【APP热更新】zip 与 APK 共用代理下载---

    @GetMapping("/admin")
    @Operation(summary = "APP版本-管理端查询")
    @RequiresPermissions("homeai:app:version:edit")
    public Result<?> adminGet() {
        return Result.OK(appVersionService.toAdminView(appVersionService.requireCurrent()));
    }

    @PutMapping("/admin")
    @AutoLog(value = "APP版本-保存")
    @Operation(summary = "APP版本-管理端保存")
    @RequiresPermissions("homeai:app:version:edit")
    public Result<?> adminSave(@RequestBody HomeaiAppVersion body) {
        try {
            appVersionService.saveCurrent(body);
            return Result.OK("保存成功");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/upload")
    @AutoLog(value = "APP版本-上传安装包")
    @Operation(summary = "APP版本-上传 APK 或 H5 zip")
    @RequiresPermissions("homeai:app:version:edit")
    public Result<?> upload(@RequestParam("file") MultipartFile file, @RequestParam("kind") String kind) {
        try {
            Map<String, String> result = appVersionService.uploadPackage(file, kind);
            return Result.OK(result);
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }
}
