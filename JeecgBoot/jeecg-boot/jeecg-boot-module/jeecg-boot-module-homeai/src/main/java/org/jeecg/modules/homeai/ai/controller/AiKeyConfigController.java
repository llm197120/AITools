package org.jeecg.modules.homeai.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.alibaba.fastjson.JSON;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.homeai.ai.entity.AiKeyConfig;
import org.jeecg.modules.homeai.ai.service.IAiKeyConfigService;
import org.jeecg.modules.homeai.ai.service.IAiQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI密钥配置管理（管理端）
 */
@Slf4j
@RestController
@RequestMapping("/homeai/ai/key-config")
public class AiKeyConfigController {

    @Autowired
    private IAiKeyConfigService keyConfigService;

    @Autowired
    private IAiQuotaService quotaService;

    /**
     * 密钥列表
     */
    @GetMapping("/list")
    public Result<?> list(AiKeyConfig config,
                          @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                          HttpServletRequest req) {
        QueryWrapper<AiKeyConfig> queryWrapper = QueryGenerator.initQueryWrapper(config, req.getParameterMap());
        Page<AiKeyConfig> page = new Page<>(pageNo, pageSize);
        IPage<AiKeyConfig> pageList = keyConfigService.page(page, queryWrapper);
        // 脱敏 API Key
        pageList.getRecords().forEach(item -> {
            if (item.getApiKeyEncrypted() != null && item.getApiKeyEncrypted().length() > 8) {
                item.setApiKeyEncrypted("sk-****" + item.getApiKeyEncrypted().substring(
                        Math.max(0, item.getApiKeyEncrypted().length() - 4)));
            }
        });
        return Result.OK(pageList);
    }

    /**
     * 新增密钥（rawApiKey 明文传入，后端加密存储）
     * 前端以 JSON body 发送，字段 apiKeyRaw 为明文密钥
     */
    @PostMapping
    public Result<?> add(@RequestBody Map<String, Object> body) {
        String rawApiKey = (String) body.remove("apiKeyRaw");
        if (rawApiKey == null || rawApiKey.trim().isEmpty()) {
            return Result.error("API Key 不能为空");
        }
        AiKeyConfig config = JSON.parseObject(JSON.toJSONString(body), AiKeyConfig.class);
        if (!keyConfigService.saveWithEncryption(config, rawApiKey.trim())) {
            return Result.error("API Key 加密存储失败");
        }
        return Result.OK("新增成功");
    }

    /**
     * 编辑密钥
     * apiKeyRaw 非空时重新加密保存；为空表示不修改密钥。
     * 前端传入的脱敏 apiKeyEncrypted 字段一律忽略，防止覆盖真实密文。
     */
    @PutMapping
    public Result<?> edit(@RequestBody Map<String, Object> body) {
        AiKeyConfig config = JSON.parseObject(JSON.toJSONString(body), AiKeyConfig.class);
        if (config == null || config.getId() == null) {
            return Result.error("参数异常");
        }
        AiKeyConfig existing = keyConfigService.getById(config.getId());
        if (existing == null) {
            return Result.error("密钥配置不存在");
        }
        // 忽略前端回传的密文/脱敏字段
        config.setApiKeyEncrypted(null);
        // 若提交了新密钥则重新加密存储
        String rawApiKey = (String) body.get("apiKeyRaw");
        if (rawApiKey != null && !rawApiKey.trim().isEmpty()) {
            try {
                String encrypted = keyConfigService.encryptApiKey(rawApiKey.trim());
                config.setApiKeyEncrypted(encrypted);
            } catch (Exception e) {
                log.error("API Key 重新加密失败", e);
                return Result.error("API Key 加密失败");
            }
        }
        keyConfigService.updateById(config);
        return Result.OK("编辑成功");
    }

    /**
     * 删除密钥
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id) {
        keyConfigService.removeById(id);
        return Result.OK("删除成功");
    }

    /**
     * 启用/停用密钥（isEnabled 可选，未传时自动切换当前状态）
     */
    @PutMapping("/{id}/status")
    public Result<?> toggleStatus(@PathVariable String id, @RequestParam(required = false) String isEnabled) {
        AiKeyConfig config = keyConfigService.getById(id);
        if (config != null) {
            if (isEnabled == null || isEnabled.isEmpty()) {
                isEnabled = "1".equals(config.getIsEnabled()) ? "0" : "1";
            }
            config.setIsEnabled(isEnabled);
            keyConfigService.updateById(config);
        }
        return Result.OK("操作成功");
    }

    /**
     * 设为默认模型
     */
    @PutMapping("/{id}/default")
    public Result<?> setDefault(@PathVariable String id) {
        // 清除其他默认
        AiKeyConfig oldDefault = keyConfigService.getDefaultModel();
        if (oldDefault != null) {
            oldDefault.setIsDefault("0");
            keyConfigService.updateById(oldDefault);
        }
        // 设置新的默认
        AiKeyConfig config = keyConfigService.getById(id);
        if (config != null) {
            config.setIsDefault("1");
            keyConfigService.updateById(config);
        }
        return Result.OK("设置成功");
    }

    /**
     * 获取默认配额配置
     */
    @GetMapping("/quota/default")
    public Result<?> getDefaultQuota() {
        return Result.OK(quotaService.getDefaultQuota());
    }

    //update-begin---author:admin ---date:2026-07-31  for：修复Token额度配置页未登录问题，新增管理端用户Token消耗统计分页接口-----------
    /**
     * 用户Token消耗统计（管理端分页）
     */
    @GetMapping("/quota/list")
    public Result<?> getQuotaUsage(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   @RequestParam(name = "userId", required = false) String userId) {
        return Result.OK(quotaService.getUsageStats(pageNo, pageSize, userId));
    }
    //update-end---author:admin ---date:2026-07-31  for：修复Token额度配置页未登录问题，新增管理端用户Token消耗统计分页接口-----------
}
