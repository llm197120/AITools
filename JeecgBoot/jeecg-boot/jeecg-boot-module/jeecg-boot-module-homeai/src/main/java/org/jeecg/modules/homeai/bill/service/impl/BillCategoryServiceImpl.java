package org.jeecg.modules.homeai.bill.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.homeai.bill.entity.BillCategory;
import org.jeecg.modules.homeai.bill.mapper.BillCategoryMapper;
import org.jeecg.modules.homeai.bill.service.IBillCategoryService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BillCategoryServiceImpl extends ServiceImpl<BillCategoryMapper, BillCategory> implements IBillCategoryService {
    @Override
    public List<BillCategory> getEnabledCategories(String type) {
        LambdaQueryWrapper<BillCategory> q = new LambdaQueryWrapper<>();
        q.eq(BillCategory::getIsEnabled, 1);
        if (type != null) q.eq(BillCategory::getType, type);
        q.orderByAsc(BillCategory::getSortOrder);
        return list(q);
    }
}
