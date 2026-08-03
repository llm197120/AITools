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
import org.jeecg.common.util.RedisUtil;
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

    @Autowired
    private RedisUtil redisUtil;

    /** 账单月度汇总缓存 key */
    private static final String CACHE_BILL_MONTHLY = "homeai:cache:bill:monthly:%s:%s";
    /** 缓存 5 分钟 */
    private static final long CACHE_BILL_TTL = 300;

    @Override
    public BillEntry add(BillEntry entry) {
        entry.setCreateTime(new Date());
        entry.setSource(entry.getSource() != null ? entry.getSource() : "manual");
        save(entry);
        clearMonthlyCache(entry.getUserId(), entry.getBillDate() != null ? entry.getBillDate() : LocalDate.now());
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
        clearMonthlyCache(existing.getUserId(), existing.getBillDate() != null ? existing.getBillDate() : LocalDate.now());
        return existing;
    }

    @Override
    public Map<String, Object> getMonthlySummary(String userId) {
        return getMonthlySummary(userId, null);
    }

    @Override
    public Map<String, Object> getMonthlySummary(String userId, String yearMonth) {
        String cacheKey = String.format(CACHE_BILL_MONTHLY, userId,
                yearMonth == null || yearMonth.isEmpty() ? LocalDate.now().toString().substring(0, 7) : yearMonth);
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof Map) {
            return (Map<String, Object>) cached;
        }
        LocalDate now = LocalDate.now();
        if (yearMonth != null && !yearMonth.isEmpty()) {
            try {
                now = LocalDate.parse(yearMonth + "-01");
            } catch (Exception e) {
                log.warn("非法的月份参数: {}, 使用当前月", yearMonth);
            }
        }
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
        redisUtil.set(cacheKey, result, CACHE_BILL_TTL);
        return result;
    }

    @Override
    public List<BillEntry> getMonthList(String userId, String yearMonth) {
        LocalDate start = LocalDate.parse(yearMonth + "-01");
        LocalDate end = start.plusMonths(1);
        LambdaQueryWrapper<BillEntry> q = new LambdaQueryWrapper<>();
        q.eq(BillEntry::getUserId, userId).eq(BillEntry::getDelFlag, 0)
         .between(BillEntry::getBillDate, start, end).orderByDesc(BillEntry::getBillDate);
        List<BillEntry> entries = list(q);
        fillCategoryNames(entries);
        return entries;
    }

    @Override
    public void fillCategoryNames(List<BillEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        // 收集出现的分类 ID
        Set<String> categoryIds = new HashSet<>();
        for (BillEntry e : entries) {
            if (e.getCategoryId() != null && !e.getCategoryId().isEmpty()) {
                categoryIds.add(e.getCategoryId());
            }
        }
        if (categoryIds.isEmpty()) {
            return;
        }
        Map<String, BillCategory> catMap = new HashMap<>();
        for (BillCategory c : categoryService.listByIds(categoryIds)) {
            if (c != null) {
                catMap.put(c.getId(), c);
            }
        }
        for (BillEntry e : entries) {
            BillCategory c = e.getCategoryId() != null ? catMap.get(e.getCategoryId()) : null;
            if (c != null) {
                e.setCategoryName(c.getName());
            }
        }
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
        BillEntry existing = getById(id);
        // @TableLogic 字段不参与 updateById，需通过 update wrapper 显式设置
        update(new LambdaUpdateWrapper<BillEntry>()
                .eq(BillEntry::getId, id)
                .set(BillEntry::getDelFlag, 1));
        if (existing != null) {
            clearMonthlyCache(existing.getUserId() != null ? existing.getUserId() : userId,
                    existing.getBillDate() != null ? existing.getBillDate() : LocalDate.now());
        }
    }

    private void clearMonthlyCache(String userId, LocalDate date) {
        if (userId == null || date == null) {
            return;
        }
        redisUtil.del(String.format(CACHE_BILL_MONTHLY, userId, date.toString().substring(0, 7)));
    }

    @Override
    public Map<String, Object> getAdminStats(String yearMonth, String dimension) {
        LocalDate ref = LocalDate.now();
        if (yearMonth != null && !yearMonth.isEmpty()) {
            try {
                ref = LocalDate.parse(yearMonth + "-01");
            } catch (Exception ignored) {
            }
        }
        LocalDate start = ref.withDayOfMonth(1);
        LocalDate end = ref.plusMonths(1).withDayOfMonth(1);
        LambdaQueryWrapper<BillEntry> q = new LambdaQueryWrapper<>();
        q.eq(BillEntry::getDelFlag, 0).between(BillEntry::getBillDate, start, end);
        List<BillEntry> bills = list(q);

        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal totalIncome = BigDecimal.ZERO;
        for (BillEntry b : bills) {
            if ("expense".equals(b.getType())) totalExpense = totalExpense.add(b.getAmount());
            else totalIncome = totalIncome.add(b.getAmount());
        }
        // 填充分类名称
        fillCategoryNames(bills);

        // 按维度聚合
        Map<String, BigDecimal> expenseByKey = new LinkedHashMap<>();
        Map<String, BigDecimal> incomeByKey = new LinkedHashMap<>();
        Map<String, String> labelMap = new HashMap<>();
        for (BillEntry b : bills) {
            String key;
            String label;
            switch (dimension == null ? "category" : dimension) {
                case "user":
                    key = b.getUserId() != null ? b.getUserId() : "未知";
                    label = key;
                    break;
                case "month":
                    key = b.getBillDate() != null ? b.getBillDate().toString().substring(0, 7) : "未知";
                    label = key;
                    break;
                default:
                    key = b.getCategoryId() != null ? b.getCategoryId() : "未分类";
                    label = b.getCategoryName() != null ? b.getCategoryName() : key;
            }
            labelMap.put(key, label);
            if ("expense".equals(b.getType())) expenseByKey.merge(key, b.getAmount(), BigDecimal::add);
            else incomeByKey.merge(key, b.getAmount(), BigDecimal::add);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String key : expenseByKey.keySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", labelMap.getOrDefault(key, key));
            row.put("expense", expenseByKey.get(key));
            row.put("income", incomeByKey.getOrDefault(key, BigDecimal.ZERO));
            rows.add(row);
        }
        for (String key : incomeByKey.keySet()) {
            if (!expenseByKey.containsKey(key)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", labelMap.getOrDefault(key, key));
                row.put("expense", BigDecimal.ZERO);
                row.put("income", incomeByKey.get(key));
                rows.add(row);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("yearMonth", yearMonth == null || yearMonth.isEmpty() ? ref.toString().substring(0, 7) : yearMonth);
        result.put("totalExpense", totalExpense);
        result.put("totalIncome", totalIncome);
        result.put("balance", totalIncome.subtract(totalExpense));
        result.put("count", bills.size());
        result.put("rows", rows);
        return result;
    }
}
