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
    private IHomeaiFileStorageService fileStorageService;

    @GetMapping
    @Operation(summary = "APP当前版本（公开，启动页探测）")
    public Result<?> publicCurrent() {
        return Result.OK(appVersionService.toPublic(appVersionService.requireCurrent()));
    }

    //update-begin---author:cursor---date:2026-08-31---for:【APP更新】APK 代理下载（SDK 拉流），绕开 OSS ApkDownloadForbidden 预签名直链限制---
    /**
     * APK 下载（匿名公开）：OSS 默认域名禁止预签名 URL 直链分发 .apk（ApkDownloadForbidden），
     * 由后端用 SDK 拉流（Header 签名，实测放行）后转发给客户端。
     */
    @GetMapping("/package/download")
    public void downloadPackage(HttpServletResponse response) {
        HomeaiAppVersion row = appVersionService.requireCurrent();
        if (oConvertUtils.isEmpty(row.getApkUrl())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            Path path = fileStorageService.resolveLocalPath(row.getApkUrl());
            HomeaiFileMime.writeLocalFile(response, path, "homeai-" + row.getVersionCode() + ".apk", "apk");
        } catch (Exception e) {
            log.error("APK 下载失败", e);
            if (!response.isCommitted()) {
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "APK 读取失败");
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
    }
    //update-end---author:cursor---date:2026-08-31---for:【APP更新】APK 代理下载（SDK 拉流）---

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
