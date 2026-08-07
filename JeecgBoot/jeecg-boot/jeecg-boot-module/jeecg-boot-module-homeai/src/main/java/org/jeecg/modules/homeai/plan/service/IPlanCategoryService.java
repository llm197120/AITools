package org.jeecg.modules.homeai.plan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.plan.entity.PlanCategory;

import java.util.List;

public interface IPlanCategoryService extends IService<PlanCategory> {
    List<PlanCategory> getEnabledCategories();
}
