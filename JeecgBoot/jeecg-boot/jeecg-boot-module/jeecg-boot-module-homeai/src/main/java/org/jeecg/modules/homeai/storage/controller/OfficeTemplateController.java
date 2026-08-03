package org.jeecg.modules.homeai.storage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.homeai.storage.entity.OfficeTemplate;
import org.jeecg.modules.homeai.storage.service.IOfficeTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 文档模板管理（管理端）
 */
@RestController
@RequestMapping("/homeai/storage/template")
public class OfficeTemplateController {

    @Autowired
    private IOfficeTemplateService templateService;

    /** 模板列表 */
    @GetMapping("/list")
    public Result<?> list(OfficeTemplate template,
                          @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                          HttpServletRequest req) {
        QueryWrapper<OfficeTemplate> queryWrapper = QueryGenerator.initQueryWrapper(template, req.getParameterMap());
        Page<OfficeTemplate> page = new Page<>(pageNo, pageSize);
        IPage<OfficeTemplate> pageList = templateService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /** 新增模板 */
    @PostMapping
    public Result<?> add(@RequestBody OfficeTemplate template) {
        templateService.save(template);
        return Result.OK("新增成功");
    }

    /** 编辑模板 */
    @PutMapping
    public Result<?> edit(@RequestBody OfficeTemplate template) {
        templateService.updateById(template);
        return Result.OK("编辑成功");
    }

    /** 删除模板 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id) {
        templateService.removeById(id);
        return Result.OK("删除成功");
    }

    /** 设为默认 */
    @PutMapping("/{id}/default")
    public Result<?> setDefault(@PathVariable String id) {
        templateService.setDefault(id);
        return Result.OK("设置成功");
    }

    /** 获取启用的模板（小程序端） */
    @GetMapping("/enabled")
    public Result<?> getEnabled(@RequestParam(required = false) String type) {
        return Result.OK(templateService.getEnabledTemplates(type));
    }
}
