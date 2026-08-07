package org.jeecg.modules.homeai.recipe.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.recipe.entity.RecipeCategory;

import java.util.List;

public interface IRecipeCategoryService extends IService<RecipeCategory> {
    List<RecipeCategory> listAllOrdered();

    void validateCategoryId(String categoryId);

    void validateNameUnique(String name, String excludeId);

    void validateDeletable(String id);
}
