package org.jeecg.modules.homeai.bill.service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.bill.entity.BillEntry;
import java.util.List;
import java.util.Map;

public interface IBillEntryService extends IService<BillEntry> {
    BillEntry add(BillEntry entry);
    BillEntry update(BillEntry entry);
    Map<String, Object> getMonthlySummary(String userId);

    /**
     * 按月获取收支汇总（yearMonth 为空时取当前月）
     */
    Map<String, Object> getMonthlySummary(String userId, String yearMonth);
    List<BillEntry> getMonthList(String userId, String yearMonth);
    //update-begin---author:cursor---date:2026-08-22---for:【审查C】账单按月服务端分页-----------
    IPage<BillEntry> pageMonthList(Page<BillEntry> page, String userId, String yearMonth, String keyword);
    //update-end---author:cursor---date:2026-08-22---for:【审查C】账单按月服务端分页-----------
    /** 为账单列表填充分类名称 */
    void fillCategoryNames(List<BillEntry> entries);
    List<Map<String, Object>> getCategoryStats(String userId, String yearMonth);
    void softDelete(String id, String userId);

    /**
     * 管理端：账单统计报表（按分类/用户/月份聚合）
     */
    Map<String, Object> getAdminStats(String yearMonth, String dimension);
}
