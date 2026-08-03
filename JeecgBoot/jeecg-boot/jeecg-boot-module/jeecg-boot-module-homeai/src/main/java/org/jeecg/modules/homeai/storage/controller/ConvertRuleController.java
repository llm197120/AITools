package org.jeecg.modules.homeai.storage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.homeai.storage.entity.ConvertRule;
import org.jeecg.modules.homeai.storage.service.IConvertRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 格式转换规则管理（管理端）
 */
@RestController
@RequestMapping("/homeai/storage/rule")
public class ConvertRuleController {

    @Autowired
    private IConvertRuleService ruleService;

    /** 规则列表 */
    @GetMapping("/list")
    public Result<?> list(ConvertRule rule,
                          @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                          HttpServletRequest req) {
        QueryWrapper<ConvertRule> queryWrapper = QueryGenerator.initQueryWrapper(rule, req.getParameterMap());
        Page<ConvertRule> page = new Page<>(pageNo, pageSize);
        IPage<ConvertRule> pageList = ruleService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /** 新增规则 */
    @PostMapping
    public Result<?> add(@RequestBody ConvertRule rule) {
        ruleService.save(rule);
        return Result.OK("新增成功");
    }

    /** 编辑规则 */
    @PutMapping
    public Result<?> edit(@RequestBody ConvertRule rule) {
        ruleService.updateById(rule);
        return Result.OK("编辑成功");
    }

    /** 删除规则 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id) {
        ruleService.removeById(id);
        return Result.OK("删除成功");
    }

    /** 启用/停用（isEnabled 可选，未传时自动切换当前状态） */
    @PutMapping("/{id}/status")
    public Result<?> toggleStatus(@PathVariable String id, @RequestParam(required = false) String isEnabled) {
        ConvertRule rule = ruleService.getById(id);
        if (rule != null) {
            if (isEnabled == null || isEnabled.isEmpty()) {
                isEnabled = "1".equals(rule.getIsEnabled()) ? "0" : "1";
            }
            rule.setIsEnabled(isEnabled);
            ruleService.updateById(rule);
        }
        return Result.OK("操作成功");
    }

    /** 获取某格式可转换的目标格式（小程序端） */
    @GetMapping("/targets")
    public Result<?> getTargets(@RequestParam String sourceFormat) {
        List<ConvertRule> targets = ruleService.getTargetFormats(sourceFormat);
        return Result.OK(targets);
    }
}
