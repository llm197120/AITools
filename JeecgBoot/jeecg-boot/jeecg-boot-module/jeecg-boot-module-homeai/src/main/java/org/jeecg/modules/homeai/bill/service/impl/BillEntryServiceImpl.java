package org.jeecg.modules.homeai.bill.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.bill.entity.BillCategory;
import org.jeecg.modules.homeai.bill.entity.BillEntry;
import org.jeecg.modules.homeai.bill.mapper.BillEntryMapper;
import org.jeecg.modules.homeai.bill.service.IBillCategoryService;
import org.jeecg.modules.homeai.bill.service.IBillEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class BillEntryServiceImpl extends ServiceImpl<BillEntryMapper, BillEntry> implements IBillEntryService {

    @Autowired
    private IBillCategoryService categoryService;

    @Override
    public BillEntry add(BillEntry entry) {
        entry.setCreateTime(new Date());
        entry.setSource(entry.getSource() != null ? entry.getSource() : "manual");
        save(entry);
        return entry;
    }

    @Override
    public boolean save(BillEntry entity) {
        fillDefaultCategory(entity);
        return super.save(entity);
    }

    /**
     * 未指定分类时自动填充该类型的默认分类（如"其他支出/其他收入"）
     */
    private void fillDefaultCategory(BillEntry entry) {
        if (entry == null || entry.getCategoryId() != null && !entry.getCategoryId().isEmpty()) {
            return;
        }
        String type = entry.getType() != null ? entry.getType() : "expense";
        List<BillCategory> categories = categoryService.getEnabledCategories(type);
        if (categories.isEmpty()) {
            return;
        }
        BillCategory def = categories.stream()
                .filter(c -> Integer.valueOf(1).equals(c.getIsDefault()))
                .findFirst()
                .orElse(categories.get(0));
        entry.setCategoryId(def.getId());
    }

    @Override
    public BillEntry update(BillEntry entry) {
        BillEntry existing = getById(entry.getId());
        if (existing == null) throw new RuntimeException("账单不存在");
        // 只更新可修改字段，保留 version 让 MyBatis-Plus @Version 自动处理乐观锁
        existing.setAmount(entry.getAmount());
        existing.setCategoryId(entry.getCategoryId());
        existing.setBillDate(entry.getBillDate());
        existing.setType(entry.getType());
        existing.setPaymentMethod(entry.getPaymentMethod());
        existing.setRemark(entry.getRemark());
        existing.setVoucherUrl(entry.getVoucherUrl());
        if (!updateById(existing)) {
            throw new RuntimeException("该账单已被其他成员修改，请刷新后重试");
        }
        return existing;
    }

    @Override
    public Map<String, Object> getMonthlySummary(String userId) {
        LocalDate now = LocalDate.now();
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = now.plusMonths(1).withDayOfMonth(1);
        LambdaQueryWrapper<BillEntry> q = new LambdaQueryWrapper<>();
        q.eq(BillEntry::getUserId, userId).eq(BillEntry::getDelFlag, 0)
         .between(BillEntry::getBillDate, start, end);
        List<BillEntry> bills = list(q);
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal totalIncome = BigDecimal.ZERO;
        for (BillEntry b : bills) {
            if ("expense".equals(b.getType())) totalExpense = totalExpense.add(b.getAmount());
            else totalIncome = totalIncome.add(b.getAmount());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalExpense", totalExpense);
        result.put("totalIncome", totalIncome);
        result.put("balance", totalIncome.subtract(totalExpense));
        result.put("count", bills.size());
        return result;
    }

    @Override
    public List<BillEntry> getMonthList(String userId, String yearMonth) {
        LocalDate start = LocalDate.parse(yearMonth + "-01");
        LocalDate end = start.plusMonths(1);
        LambdaQueryWrapper<BillEntry> q = new LambdaQueryWrapper<>();
        q.eq(BillEntry::getUserId, userId).eq(BillEntry::getDelFlag, 0)
         .between(BillEntry::getBillDate, start, end).orderByDesc(BillEntry::getBillDate);
        return list(q);
    }

    @Override
    public List<Map<String, Object>> getCategoryStats(String userId, String yearMonth) {
        LocalDate start = LocalDate.parse(yearMonth + "-01");
        LocalDate end = start.plusMonths(1);
        LambdaQueryWrapper<BillEntry> q = new LambdaQueryWrapper<>();
        q.eq(BillEntry::getUserId, userId).eq(BillEntry::getDelFlag, 0).eq(BillEntry::getType, "expense")
         .between(BillEntry::getBillDate, start, end);
        List<BillEntry> bills = list(q);
        Map<String, BigDecimal> stats = new LinkedHashMap<>();
        for (BillEntry b : bills) {
            stats.merge(b.getCategoryId(), b.getAmount(), BigDecimal::add);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        stats.forEach((k, v) -> { Map<String, Object> m = new HashMap<>(); m.put("categoryId", k); m.put("amount", v); result.add(m); });
        return result;
    }

    @Override
    public void softDelete(String id, String userId) {
        // @TableLogic 字段不参与 updateById，需通过 update wrapper 显式设置
        update(new LambdaUpdateWrapper<BillEntry>()
                .eq(BillEntry::getId, id)
                .set(BillEntry::getDelFlag, 1));
    }
}
