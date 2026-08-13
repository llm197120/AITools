package org.jeecg.modules.homeai.config.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.homeai.config.dto.HomeaiPlanConfigDto;
import org.jeecg.modules.homeai.config.dto.HomeaiStorageConfigDto;
import org.jeecg.modules.homeai.config.entity.HomeaiFileWhitelist;
import org.jeecg.modules.homeai.config.service.IHomeaiFileWhitelistService;
import org.jeecg.modules.homeai.config.service.IHomeaiPlanConfigService;
import org.jeecg.modules.homeai.config.service.IHomeaiStorageConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/homeai/config")
public class HomeaiConfigController {

    @Value("${homeai.wechat.plan-remind-template-id:}")
    private String planRemindTemplateId;

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】学习提醒模板-----------
    @Value("${homeai.wechat.learn-remind-template-id:}")
    private String learnRemindTemplateId;
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29】学习提醒模板-----------

    @Autowired
    private IHomeaiFileWhitelistService whitelistService;

    @Autowired
    private IHomeaiPlanConfigService planConfigService;

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】存储配额配置 API-----------
    @Autowired
    private IHomeaiStorageConfigService storageConfigService;
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】存储配额配置 API-----------

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R31】学习提醒模板联调-----------
    @Autowired
    private org.jeecg.modules.homeai.config.service.IHomeaiWxSubscribeService wxSubscribeService;
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R31】学习提醒模板联调-----------

    //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R32】家庭配额运营看板-----------
    @Autowired
    private org.jeecg.modules.homeai.storage.service.IStorageFileService storageFileService;
    //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R32】家庭配额运营看板-----------

    @GetMapping("/file-whitelist")
    @Operation(summary = "文件白名单-查询(小程序/管理端)")
    public Result<?> getFileWhitelist() {
        List<HomeaiFileWhitelist> items = whitelistService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HomeaiFileWhitelist>()
                        .orderByAsc(HomeaiFileWhitelist::getSortOrder));
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("extensions", whitelistService.getEnabledExtensions());
        return Result.OK(data);
    }

    @PutMapping("/file-whitelist")
    @AutoLog(value = "文件白名单-更新")
    @Operation(summary = "文件白名单-更新(管理端)")
    @RequiresPermissions("homeai:config:whitelist:edit")
    public Result<?> updateFileWhitelist(@RequestBody List<HomeaiFileWhitelist> items) {
        try {
            whitelistService.replaceAll(items);
            return Result.OK("保存成功");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/plan")
    @Operation(summary = "计划模块配置-查询(管理端)")
    @RequiresPermissions("homeai:config:plan:list")
    public Result<?> getPlanConfig() {
        return Result.OK(planConfigService.getConfig());
    }

    @PutMapping("/plan")
    @AutoLog(value = "计划模块配置-更新")
    @Operation(summary = "计划模块配置-更新(管理端)")
    @RequiresPermissions("homeai:config:plan:edit")
    public Result<?> updatePlanConfig(@RequestBody HomeaiPlanConfigDto config) {
        planConfigService.saveConfig(config);
        return Result.OK("保存成功");
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】存储配额配置 API-----------
    @GetMapping("/storage")
    @Operation(summary = "资料存储配额配置-查询(管理端)")
    @RequiresPermissions("homeai:storage:file:list")
    public Result<?> getStorageConfig() {
        return Result.OK(storageConfigService.getConfig());
    }

    @PutMapping("/storage")
    @AutoLog(value = "资料存储配额配置-更新")
    @Operation(summary = "资料存储配额配置-更新(管理端)")
    @RequiresPermissions("homeai:storage:file:list")
    public Result<?> updateStorageConfig(@RequestBody HomeaiStorageConfigDto config) {
        storageConfigService.saveConfig(config);
        return Result.OK("保存成功");
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R30】家庭级配额覆盖 API-----------
    @GetMapping("/storage/family/{familyId}")
    @Operation(summary = "家庭存储配额覆盖-查询")
    @RequiresPermissions("homeai:storage:file:list")
    public Result<?> getFamilyStorageLimit(@PathVariable String familyId) {
        Map<String, Object> data = new HashMap<>();
        data.put("familyId", familyId);
        data.put("limitBytes", storageConfigService.getFamilyLimitBytes(familyId));
        data.put("custom", storageConfigService.hasFamilyLimitOverride(familyId));
        data.put("defaultFamilyLimitBytes", storageConfigService.getDefaultFamilyLimitBytes());
        return Result.OK(data);
    }

    @PutMapping("/storage/family/{familyId}")
    @AutoLog(value = "家庭存储配额覆盖-更新")
    @Operation(summary = "家庭存储配额覆盖-更新(字节，<=0 清除)")
    @RequiresPermissions("homeai:storage:file:list")
    public Result<?> updateFamilyStorageLimit(@PathVariable String familyId,
                                              @RequestBody Map<String, Object> body) {
        Long limitBytes = null;
        if (body != null && body.get("limitBytes") != null) {
            limitBytes = Long.valueOf(String.valueOf(body.get("limitBytes")));
        }
        storageConfigService.setFamilyLimitBytes(familyId, limitBytes);
        Map<String, Object> data = new HashMap<>();
        data.put("familyId", familyId);
        data.put("limitBytes", storageConfigService.getFamilyLimitBytes(familyId));
        data.put("custom", storageConfigService.hasFamilyLimitOverride(familyId));
        return Result.OK("保存成功", data);
    }

    @DeleteMapping("/storage/family/{familyId}")
    @AutoLog(value = "家庭存储配额覆盖-清除")
    @Operation(summary = "家庭存储配额覆盖-清除")
    @RequiresPermissions("homeai:storage:file:list")
    public Result<?> clearFamilyStorageLimit(@PathVariable String familyId) {
        storageConfigService.clearFamilyLimitBytes(familyId);
        return Result.OK("已恢复默认家庭配额");
    }

    //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R32】家庭配额运营看板-----------
    @GetMapping("/storage/families")
    @Operation(summary = "家庭存储配额运营看板")
    @RequiresPermissions("homeai:storage:file:list")
    public Result<?> familyStorageBoard(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Boolean onlyWarn,
                                        @RequestParam(required = false) Boolean onlyCustom) {
        java.util.List<java.util.Map<String, Object>> items =
                storageFileService.listFamilyQuotaBoard(keyword, onlyWarn, onlyCustom);
        long warnCount = 0;
        long customCount = 0;
        long totalUsed = 0;
        for (java.util.Map<String, Object> row : items) {
            if (Boolean.TRUE.equals(row.get("overWarn"))) {
                warnCount++;
            }
            if (Boolean.TRUE.equals(row.get("customLimit"))) {
                customCount++;
            }
            Object size = row.get("totalSize");
            if (size instanceof Number) {
                totalUsed += ((Number) size).longValue();
            }
        }
        java.util.Map<String, Object> data = new HashMap<>();
        data.put("defaultFamilyLimitBytes", storageConfigService.getDefaultFamilyLimitBytes());
        data.put("warnPercent", storageConfigService.getWarnPercent());
        java.util.Map<String, Object> summary = new HashMap<>();
        summary.put("familyCount", items.size());
        summary.put("warnCount", warnCount);
        summary.put("customCount", customCount);
        summary.put("totalUsed", totalUsed);
        data.put("summary", summary);
        data.put("items", items);
        return Result.OK(data);
    }

    @PutMapping("/storage/families/batch")
    @AutoLog(value = "家庭存储配额-批量调整")
    @Operation(summary = "家庭存储配额批量调整（items 设覆盖，resetIds 恢复默认）")
    @RequiresPermissions("homeai:storage:file:list")
    public Result<?> batchFamilyStorageLimit(@RequestBody Map<String, Object> body) {
        int updated = 0;
        int reset = 0;
        if (body != null) {
            Object itemsObj = body.get("items");
            if (itemsObj instanceof java.util.List<?> items) {
                for (Object item : items) {
                    if (!(item instanceof Map<?, ?> row)) {
                        continue;
                    }
                    Object fid = row.get("familyId");
                    if (fid == null) {
                        continue;
                    }
                    Long limitBytes = null;
                    if (row.get("limitBytes") != null) {
                        limitBytes = Long.valueOf(String.valueOf(row.get("limitBytes")));
                    }
                    storageConfigService.setFamilyLimitBytes(String.valueOf(fid), limitBytes);
                    updated++;
                }
            }
            Object resetObj = body.get("resetIds");
            if (resetObj instanceof java.util.List<?> ids) {
                for (Object id : ids) {
                    if (id == null) {
                        continue;
                    }
                    storageConfigService.clearFamilyLimitBytes(String.valueOf(id));
                    reset++;
                }
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("updated", updated);
        data.put("reset", reset);
        return Result.OK("批量调整完成", data);
    }
    //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R32】家庭配额运营看板-----------
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R30】家庭级配额覆盖 API-----------
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】存储配额配置 API-----------

    @GetMapping("/wechat-public")
    @Operation(summary = "微信公开配置(小程序订阅消息等)")
    public Result<?> getWechatPublicConfig() {
        Map<String, Object> data = new HashMap<>();
        data.put("planRemindTemplateId", planRemindTemplateId != null ? planRemindTemplateId : "");
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】学习提醒模板-----------
        data.put("learnRemindTemplateId", learnRemindTemplateId != null ? learnRemindTemplateId : "");
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29】学习提醒模板-----------
        return Result.OK(data);
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R31】学习提醒模板联调-----------
    @GetMapping("/wechat-learn-remind")
    @Operation(summary = "学习提醒订阅消息字段映射与样例(管理端联调)")
    @RequiresPermissions("homeai:learn:material:list")
    public Result<?> getLearnRemindTemplateMeta() {
        return Result.OK(wxSubscribeService.describeLearnRemindTemplate());
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R31】学习提醒模板联调-----------
}
