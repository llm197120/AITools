package org.jeecg.modules.homeai.recipe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.homeai.recipe.entity.RecipeCategory;
import org.jeecg.modules.homeai.recipe.mapper.RecipeCategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 菜谱分类管理（管理端）
 */
@Slf4j
@RestController
@RequestMapping("/homeai/recipe/category")
public class RecipeCategoryController {

    @Autowired
    private RecipeCategoryMapper categoryMapper;

    @GetMapping("/list")
    @Operation(summary="菜谱分类-分页列表查询")
    @RequiresPermissions("homeai:recipe:category:list")
    public Result<?> list(@RequestParam(defaultValue = "1") int pageNo,
                          @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<RecipeCategory> q = new LambdaQueryWrapper<>();
        q.orderByAsc(RecipeCategory::getSortOrder);
        IPage<RecipeCategory> page = categoryMapper.selectPage(new Page<>(pageNo, pageSize), q);
        return Result.OK(page);
    }

    @GetMapping("/all")
    @Operation(summary="菜谱分类-全部列表")
    public Result<?> all() {
        LambdaQueryWrapper<RecipeCategory> q = new LambdaQueryWrapper<>();
        q.orderByAsc(RecipeCategory::getSortOrder);
        return Result.OK(categoryMapper.selectList(q));
    }

    @PostMapping
    @AutoLog(value="菜谱分类-新增")
    @Operation(summary="菜谱分类-新增")
    @RequiresPermissions("homeai:recipe:category:add")
    public Result<?> add(@RequestBody RecipeCategory category) {
        category.setIsDefault(category.getIsDefault() != null ? category.getIsDefault() : 0);
        category.setCreateTime(new Date());
        categoryMapper.insert(category);
        return Result.OK("新增成功");
    }

    @PutMapping
    @AutoLog(value="菜谱分类-编辑")
    @Operation(summary="菜谱分类-编辑")
    @RequiresPermissions("homeai:recipe:category:edit")
    public Result<?> edit(@RequestBody RecipeCategory category) {
        categoryMapper.updateById(category);
        return Result.OK("编辑成功");
    }

    @DeleteMapping("/{id}")
    @AutoLog(value="菜谱分类-删除")
    @Operation(summary="菜谱分类-删除")
    @RequiresPermissions("homeai:recipe:category:delete")
    public Result<?> delete(@PathVariable String id) {
        RecipeCategory category = categoryMapper.selectById(id);
        if (category != null && Integer.valueOf(1).equals(category.getIsDefault())) {
            return Result.error("系统默认分类不可删除");
        }
        categoryMapper.deleteById(id);
        return Result.OK("删除成功");
    }
}
