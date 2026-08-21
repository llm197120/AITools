package org.jeecg.modules.homeai.recipe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.recipe.entity.Recipe;
import org.jeecg.modules.homeai.recipe.entity.RecipeCategory;
import org.jeecg.modules.homeai.recipe.mapper.RecipeCategoryMapper;
import org.jeecg.modules.homeai.recipe.mapper.RecipeMapper;
import org.jeecg.modules.homeai.recipe.service.IRecipeCategoryService;
import org.jeecg.modules.homeai.recipe.util.RecipeCategoryResolve;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecipeCategoryServiceImpl extends ServiceImpl<RecipeCategoryMapper, RecipeCategory>
        implements IRecipeCategoryService {

    @Autowired
    private RecipeMapper recipeMapper;

    @Override
    public List<RecipeCategory> listAllOrdered() {
        LambdaQueryWrapper<RecipeCategory> q = new LambdaQueryWrapper<>();
        q.orderByAsc(RecipeCategory::getSortOrder);
        return list(q);
    }

    @Override
    public void validateCategoryId(String categoryId) {
        if (oConvertUtils.isEmpty(categoryId)) {
            throw new JeecgBootException("请选择菜谱分类");
        }
        RecipeCategory category = getById(categoryId);
        if (category == null) {
            throw new JeecgBootException("菜谱分类不存在");
        }
    }

    @Override
    public void validateNameUnique(String name, String excludeId) {
        if (oConvertUtils.isEmpty(name)) {
            throw new JeecgBootException("分类名称不能为空");
        }
        LambdaQueryWrapper<RecipeCategory> q = new LambdaQueryWrapper<>();
        q.eq(RecipeCategory::getName, name.trim());
        if (oConvertUtils.isNotEmpty(excludeId)) {
            q.ne(RecipeCategory::getId, excludeId);
        }
        if (count(q) > 0) {
            throw new JeecgBootException("分类名称已存在");
        }
    }

    @Override
    public void validateDeletable(String id) {
        RecipeCategory category = getById(id);
        if (category == null) {
            throw new JeecgBootException("分类不存在");
        }
        if (Integer.valueOf(1).equals(category.getIsDefault())) {
            throw new JeecgBootException("系统默认分类不可删除");
        }
        Long used = recipeMapper.selectCount(new LambdaQueryWrapper<Recipe>()
                .eq(Recipe::getCategoryId, id)
                .eq(Recipe::getDelFlag, 0));
        if (used != null && used > 0) {
            throw new JeecgBootException("该分类下仍有菜谱，无法删除");
        }
    }

    //update-begin---author:cursor---date:2026-08-21---for:【菜谱导入】分类ID/中文名解析并按菜名纠正---
    @Override
    public String resolveImportCategoryId(String rawCategory, String recipeName) {
        List<RecipeCategory> all = listAllOrdered();
        if (all == null || all.isEmpty()) {
            throw new JeecgBootException("未配置菜谱分类，请先初始化默认分类");
        }
        Set<String> ids = all.stream().map(RecipeCategory::getId).collect(Collectors.toSet());
        Map<String, String> nameToId = new LinkedHashMap<>();
        for (RecipeCategory c : all) {
            if (c.getName() != null) {
                nameToId.putIfAbsent(c.getName().trim(), c.getId());
            }
        }
        try {
            return RecipeCategoryResolve.resolve(rawCategory, recipeName, ids, nameToId);
        } catch (IllegalArgumentException e) {
            throw new JeecgBootException(e.getMessage());
        }
    }
    //update-end---author:cursor---date:2026-08-21---for:【菜谱导入】分类ID/中文名解析并按菜名纠正---
}
