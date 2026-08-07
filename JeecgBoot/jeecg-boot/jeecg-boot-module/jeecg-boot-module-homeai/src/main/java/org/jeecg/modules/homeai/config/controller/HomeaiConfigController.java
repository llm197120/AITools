package org.jeecg.modules.homeai.config.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.homeai.config.dto.HomeaiPlanConfigDto;
import org.jeecg.modules.homeai.config.entity.HomeaiFileWhitelist;
import org.jeecg.modules.homeai.config.service.IHomeaiFileWhitelistService;
import org.jeecg.modules.homeai.config.service.IHomeaiPlanConfigService;
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

    @Autowired
    private IHomeaiFileWhitelistService whitelistService;

    @Autowired
    private IHomeaiPlanConfigService planConfigService;

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

    @GetMapping("/wechat-public")
    @Operation(summary = "微信公开配置(小程序订阅消息等)")
    public Result<?> getWechatPublicConfig() {
        Map<String, Object> data = new HashMap<>();
        data.put("planRemindTemplateId", planRemindTemplateId != null ? planRemindTemplateId : "");
        return Result.OK(data);
    }
}
