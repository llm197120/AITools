package org.jeecg.modules.homeai.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.homeai.plan.entity.PlanCategory;
import org.jeecg.modules.homeai.plan.mapper.PlanCategoryMapper;
import org.jeecg.modules.homeai.plan.service.IPlanCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlanCategoryServiceImpl extends ServiceImpl<PlanCategoryMapper, PlanCategory> implements IPlanCategoryService {

    @Override
    public List<PlanCategory> getEnabledCategories() {
        LambdaQueryWrapper<PlanCategory> q = new LambdaQueryWrapper<>();
        q.eq(PlanCategory::getIsEnabled, 1).orderByAsc(PlanCategory::getSortOrder);
        return list(q);
    }
}
