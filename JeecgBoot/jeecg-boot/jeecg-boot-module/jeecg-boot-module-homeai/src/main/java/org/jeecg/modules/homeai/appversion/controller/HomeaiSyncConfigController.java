package org.jeecg.modules.homeai.appversion.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.appversion.entity.HomeaiSyncConfig;
import org.jeecg.modules.homeai.appversion.mapper.HomeaiSyncConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * HomeAI 离线同步与缓存配置：App 启动拉取（公开），管理端修改（需权限）。
 */
@Slf4j
@RestController
@RequestMapping("/homeai/config/sync")
public class HomeaiSyncConfigController {

    @Autowired
    private HomeaiSyncConfigMapper mapper;

    private HomeaiSyncConfig requireCurrent() {
        HomeaiSyncConfig cfg = mapper.selectById(HomeaiSyncConfig.CURRENT_ID);
        if (cfg != null) {
            return cfg;
        }
        HomeaiSyncConfig seed = new HomeaiSyncConfig();
        seed.setId(HomeaiSyncConfig.CURRENT_ID);
        seed.setBatchSize(1);
        seed.setIntervalMs(5000);
        seed.setMaxRetriesPerDay(20);
        seed.setImageCacheLimitMb(4096);
        Date now = new Date();
        seed.setCreateTime(now);
        seed.setUpdateTime(now);
        try {
            mapper.insert(seed);
        } catch (Exception e) {
            log.warn("同步配置 seed 写入失败（可能并发）：{}", e.getMessage());
        }
        return seed;
    }

    @GetMapping
    @Operation(summary = "离线同步配置（App 公开拉取）")
    public Result<?> get() {
        return Result.OK(requireCurrent());
    }

    @PutMapping("/admin")
    @Operation(summary = "离线同步配置（管理端修改）")
    @RequiresPermissions("homeai:app:version:edit")
    public Result<?> save(@RequestBody HomeaiSyncConfig body) {
        HomeaiSyncConfig cfg = requireCurrent();
        if (body.getBatchSize() != null && body.getBatchSize() > 0) {
            cfg.setBatchSize(body.getBatchSize());
        }
        if (body.getIntervalMs() != null && body.getIntervalMs() >= 1000) {
            cfg.setIntervalMs(body.getIntervalMs());
        }
        if (body.getMaxRetriesPerDay() != null && body.getMaxRetriesPerDay() > 0) {
            cfg.setMaxRetriesPerDay(body.getMaxRetriesPerDay());
        }
        if (body.getImageCacheLimitMb() != null && body.getImageCacheLimitMb() > 0) {
            cfg.setImageCacheLimitMb(body.getImageCacheLimitMb());
        }
        if (oConvertUtils.isEmpty(cfg.getBatchSize()) || cfg.getBatchSize() == null) {
            throw new JeecgBootException("参数不合法");
        }
        cfg.setUpdateTime(new Date());
        mapper.updateById(cfg);
        return Result.OK("保存成功");
    }
}