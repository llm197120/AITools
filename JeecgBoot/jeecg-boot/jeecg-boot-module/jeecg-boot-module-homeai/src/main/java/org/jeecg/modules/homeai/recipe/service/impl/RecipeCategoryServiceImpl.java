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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
