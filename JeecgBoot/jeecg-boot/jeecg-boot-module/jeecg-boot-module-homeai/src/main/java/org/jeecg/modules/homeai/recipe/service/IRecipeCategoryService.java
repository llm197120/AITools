package org.jeecg.modules.homeai.recipe.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.recipe.entity.RecipeCategory;

import java.util.List;

public interface IRecipeCategoryService extends IService<RecipeCategory> {
    List<RecipeCategory> listAllOrdered();

    void validateCategoryId(String categoryId);

    void validateNameUnique(String name, String excludeId);

    void validateDeletable(String id);

    //update-begin---author:cursor---date:2026-08-21---for:【菜谱导入】分类ID/中文名解析并按菜名纠正---
    /** Excel 分类列：支持 ID 或中文名，并结合菜名纠正过粗分类 */
    String resolveImportCategoryId(String rawCategory, String recipeName);
    //update-end---author:cursor---date:2026-08-21---for:【菜谱导入】分类ID/中文名解析并按菜名纠正---
}
