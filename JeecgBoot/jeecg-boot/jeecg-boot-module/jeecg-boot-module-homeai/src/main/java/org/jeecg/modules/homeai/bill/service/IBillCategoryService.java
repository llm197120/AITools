package org.jeecg.modules.homeai.bill.service;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.bill.entity.BillCategory;
import java.util.List;

public interface IBillCategoryService extends IService<BillCategory> {
    List<BillCategory> getEnabledCategories(String type);
}
