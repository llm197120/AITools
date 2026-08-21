package org.jeecg.modules.homeai.appversion.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.homeai.appversion.entity.HomeaiAppVersion;
import org.jeecg.modules.homeai.appversion.service.IHomeaiAppVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/homeai/app/version")
public class HomeaiAppVersionController {

    @Autowired
    private IHomeaiAppVersionService appVersionService;

    @GetMapping
    @Operation(summary = "APP当前版本（公开，启动页探测）")
    public Result<?> publicCurrent() {
        return Result.OK(appVersionService.toPublic(appVersionService.requireCurrent()));
    }

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
