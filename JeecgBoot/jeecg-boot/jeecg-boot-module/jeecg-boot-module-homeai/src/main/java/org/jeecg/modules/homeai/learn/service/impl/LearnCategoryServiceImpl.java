package org.jeecg.modules.homeai.learn.service.impl;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.jeecg.common.exception.JeecgBootException;

import org.jeecg.common.util.oConvertUtils;

import org.jeecg.modules.homeai.learn.entity.LearnCategory;

import org.jeecg.modules.homeai.learn.mapper.LearnCategoryMapper;

import org.jeecg.modules.homeai.learn.service.ILearnCategoryService;

import org.jeecg.modules.homeai.recipe.entity.LearnMaterial;

import org.jeecg.modules.homeai.recipe.mapper.LearnMaterialMapper;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;



import java.util.List;



@Service

public class LearnCategoryServiceImpl extends ServiceImpl<LearnCategoryMapper, LearnCategory>

        implements ILearnCategoryService {



    @Autowired

    private LearnMaterialMapper learnMaterialMapper;



    @Override

    public List<LearnCategory> getEnabledCategories() {

        LambdaQueryWrapper<LearnCategory> q = new LambdaQueryWrapper<>();

        q.eq(LearnCategory::getIsEnabled, 1).orderByAsc(LearnCategory::getSortOrder);

        return list(q);

    }



    @Override

    public void validateNameUnique(String name, String excludeId) {

        if (oConvertUtils.isEmpty(name)) {

            throw new JeecgBootException("分类名称不能为空");

        }

        LambdaQueryWrapper<LearnCategory> q = new LambdaQueryWrapper<>();

        q.eq(LearnCategory::getName, name.trim());

        if (oConvertUtils.isNotEmpty(excludeId)) {

            q.ne(LearnCategory::getId, excludeId);

        }

        if (count(q) > 0) {

            throw new JeecgBootException("分类名称已存在");

        }

    }



    @Override

    public void validateDeletable(String id) {

        LearnCategory category = getById(id);

        if (category == null) {

            throw new JeecgBootException("分类不存在");

        }

        if (Integer.valueOf(1).equals(category.getIsDefault())) {

            throw new JeecgBootException("系统默认分类不可删除");

        }

        Long used = learnMaterialMapper.selectCount(new LambdaQueryWrapper<LearnMaterial>()

                .and(w -> w.eq(LearnMaterial::getCategoryId, id)
                        .or(n -> n.eq(LearnMaterial::getCategory, category.getName())))

                .eq(LearnMaterial::getDelFlag, 0));

        if (used != null && used > 0) {

            throw new JeecgBootException("该分类下仍有学习资料，无法删除");

        }

    }

    @Override
    public void validateCategoryId(String categoryId) {
        if (oConvertUtils.isEmpty(categoryId)) {
            return;
        }
        LearnCategory category = getById(categoryId);
        if (category == null || !Integer.valueOf(1).equals(category.getIsEnabled())) {
            throw new JeecgBootException("学习分类不存在或已停用");
        }
    }

    @Override
    public String resolveCategoryName(String categoryId) {
        if (oConvertUtils.isEmpty(categoryId)) {
            return null;
        }
        LearnCategory category = getById(categoryId);
        return category != null ? category.getName() : null;
    }

}

