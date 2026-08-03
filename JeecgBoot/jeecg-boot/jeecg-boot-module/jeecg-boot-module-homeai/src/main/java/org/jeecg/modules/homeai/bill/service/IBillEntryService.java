package org.jeecg.modules.homeai.bill.service;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.bill.entity.BillEntry;
import java.util.List;
import java.util.Map;

public interface IBillEntryService extends IService<BillEntry> {
    BillEntry add(BillEntry entry);
    BillEntry update(BillEntry entry);
    Map<String, Object> getMonthlySummary(String userId);
    List<BillEntry> getMonthList(String userId, String yearMonth);
    List<Map<String, Object>> getCategoryStats(String userId, String yearMonth);
    void softDelete(String id, String userId);
}
