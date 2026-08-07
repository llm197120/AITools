package org.jeecg.modules.homeai.recipe.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.homeai.recipe.entity.RecipeCategory;
import org.jeecg.modules.homeai.recipe.service.IRecipeCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 菜谱分类管理（管理端）
 */
@Slf4j
@RestController
@RequestMapping("/homeai/recipe/category")
public class RecipeCategoryController {

    @Autowired
    private IRecipeCategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "菜谱分类-分页列表查询")
    @RequiresPermissions("homeai:recipe:category:list")
    public Result<?> list(@RequestParam(defaultValue = "1") int pageNo,
                          @RequestParam(defaultValue = "10") int pageSize) {
        IPage<RecipeCategory> page = categoryService.page(new Page<>(pageNo, pageSize),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RecipeCategory>()
                        .orderByAsc(RecipeCategory::getSortOrder));
        return Result.OK(page);
    }

    @GetMapping("/all")
    @Operation(summary = "菜谱分类-全部列表（下拉选项）")
    public Result<?> all() {
        return Result.OK(categoryService.listAllOrdered());
    }

    @PostMapping
    @AutoLog(value = "菜谱分类-新增")
    @Operation(summary = "菜谱分类-新增")
    @RequiresPermissions("homeai:recipe:category:add")
    public Result<?> add(@RequestBody RecipeCategory category) {
        try {
            categoryService.validateNameUnique(category.getName(), null);
            category.setIsDefault(category.getIsDefault() != null ? category.getIsDefault() : 0);
            category.setCreateTime(new Date());
            categoryService.save(category);
            return Result.OK("新增成功");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping
    @AutoLog(value = "菜谱分类-编辑")
    @Operation(summary = "菜谱分类-编辑")
    @RequiresPermissions("homeai:recipe:category:edit")
    public Result<?> edit(@RequestBody RecipeCategory category) {
        try {
            categoryService.validateNameUnique(category.getName(), category.getId());
            categoryService.updateById(category);
            return Result.OK("编辑成功");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @AutoLog(value = "菜谱分类-删除")
    @Operation(summary = "菜谱分类-删除")
    @RequiresPermissions("homeai:recipe:category:delete")
    public Result<?> delete(@PathVariable String id) {
        try {
            categoryService.validateDeletable(id);
            categoryService.removeById(id);
            return Result.OK("删除成功");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }
}
