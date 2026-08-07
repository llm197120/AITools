package org.jeecg.modules.homeai.learn.controller;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.core.metadata.IPage;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.swagger.v3.oas.annotations.Operation;

import lombok.extern.slf4j.Slf4j;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import org.jeecg.common.api.vo.Result;

import org.jeecg.common.aspect.annotation.AutoLog;

import org.jeecg.common.exception.JeecgBootException;

import org.jeecg.modules.homeai.learn.entity.LearnCategory;

import org.jeecg.modules.homeai.learn.service.ILearnCategoryService;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;



import java.util.Date;



@Slf4j

@RestController

@RequestMapping("/homeai/learn/category")

public class LearnCategoryController {



    @Autowired

    private ILearnCategoryService categoryService;



    @GetMapping("/list")

    @Operation(summary = "学习分类-分页列表(管理端)")

    @RequiresPermissions("homeai:learn:category:list")

    public Result<?> list(@RequestParam(defaultValue = "1") int pageNo,

                          @RequestParam(defaultValue = "10") int pageSize) {

        IPage<LearnCategory> page = categoryService.page(new Page<>(pageNo, pageSize),

                new LambdaQueryWrapper<LearnCategory>().orderByAsc(LearnCategory::getSortOrder));

        return Result.OK(page);

    }



    @GetMapping("/all")

    @Operation(summary = "学习分类-启用列表(下拉)")

    public Result<?> all() {

        return Result.OK(categoryService.getEnabledCategories());

    }



    @PostMapping

    @AutoLog(value = "学习分类-新增")

    @Operation(summary = "学习分类-新增")

    @RequiresPermissions("homeai:learn:category:add")

    public Result<?> add(@RequestBody LearnCategory category) {

        try {

            categoryService.validateNameUnique(category.getName(), null);

            if (category.getIsEnabled() == null) category.setIsEnabled(1);

            if (category.getSortOrder() == null) category.setSortOrder(0);

            if (category.getIsDefault() == null) category.setIsDefault(0);

            category.setCreateTime(new Date());

            categoryService.save(category);

            return Result.OK("新增成功");

        } catch (JeecgBootException e) {

            return Result.error(e.getMessage());

        }

    }



    @PutMapping

    @AutoLog(value = "学习分类-编辑")

    @Operation(summary = "学习分类-编辑")

    @RequiresPermissions("homeai:learn:category:edit")

    public Result<?> edit(@RequestBody LearnCategory category) {

        try {

            categoryService.validateNameUnique(category.getName(), category.getId());

            categoryService.updateById(category);

            return Result.OK("编辑成功");

        } catch (JeecgBootException e) {

            return Result.error(e.getMessage());

        }

    }



    @DeleteMapping("/{id}")

    @AutoLog(value = "学习分类-删除")

    @Operation(summary = "学习分类-删除")

    @RequiresPermissions("homeai:learn:category:delete")

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

