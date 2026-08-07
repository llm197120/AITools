package org.jeecg.modules.homeai.learn.service;



import com.baomidou.mybatisplus.extension.service.IService;

import org.jeecg.modules.homeai.learn.entity.LearnCategory;



import java.util.List;



public interface ILearnCategoryService extends IService<LearnCategory> {



    List<LearnCategory> getEnabledCategories();



    void validateNameUnique(String name, String excludeId);



    void validateDeletable(String id);

    void validateCategoryId(String categoryId);

    String resolveCategoryName(String categoryId);
}

