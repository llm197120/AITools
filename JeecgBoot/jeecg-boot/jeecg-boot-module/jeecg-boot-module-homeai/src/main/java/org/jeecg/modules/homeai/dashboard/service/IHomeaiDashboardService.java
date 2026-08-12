package org.jeecg.modules.homeai.dashboard.service;

import java.util.Map;

/**
 * 管理端综合仪表盘
 */
public interface IHomeaiDashboardService {

    /**
     * 计划完成率 + 学习时长交叉统计
     *
     * @param yearMonth YYYY-MM，可空（默认当月）
     * @param days      学习趋势天数
     * @param userId    可选用户筛选（仅影响计划完成率）
     */
    Map<String, Object> planLearn(String yearMonth, int days, String userId);
}
