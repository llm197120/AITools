package org.jeecg.modules.homeai.dashboard.service.impl;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.dashboard.service.IHomeaiDashboardService;
import org.jeecg.modules.homeai.plan.service.IPlanService;
import org.jeecg.modules.homeai.recipe.service.ILearnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 聚合计划完成率与学习统计
 */
@Service
public class HomeaiDashboardServiceImpl implements IHomeaiDashboardService {

    @Autowired
    private IPlanService planService;

    @Autowired
    private ILearnService learnService;

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R26】计划+学习交叉统计-----------
    @Override
    public Map<String, Object> planLearn(String yearMonth, int days, String userId) {
        String ym = oConvertUtils.isEmpty(yearMonth) ? YearMonth.now().toString() : yearMonth.trim();
        int trendDays = days <= 0 ? 30 : Math.min(days, 90);

        List<Map<String, Object>> byUser = planService.getCompletionStats(userId, ym);
        int total = 0;
        int completed = 0;
        if (byUser != null) {
            for (Map<String, Object> row : byUser) {
                total += toInt(row.get("total"));
                completed += toInt(row.get("completed"));
            }
        }
        int overallRate = total > 0 ? (int) Math.round(completed * 100.0 / total) : 0;

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("overallRate", overallRate);
        plan.put("total", total);
        plan.put("completed", completed);
        plan.put("byUser", byUser);

        Map<String, Object> learn = new LinkedHashMap<>();
        Map<String, Object> learnStats = learnService.adminStats();
        if (learnStats != null) {
            learn.putAll(learnStats);
        }
        learn.put("trend", learnService.adminStatsTrend(trendDays));
        learn.put("byCategory", learnService.getAdminStatsByCategory());

        Map<String, Object> result = new HashMap<>();
        result.put("yearMonth", ym);
        result.put("days", trendDays);
        result.put("plan", plan);
        result.put("learn", learn);
        return result;
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R26】计划+学习交叉统计-----------

    private static int toInt(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }
}
