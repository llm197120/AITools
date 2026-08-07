package org.jeecg.modules.homeai.plan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.plan.entity.PlanMaster;
import org.jeecg.modules.homeai.plan.entity.PlanInstance;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IPlanService extends IService<PlanMaster> {

    /** 创建计划（含首天实例） */
    PlanMaster createPlan(PlanMaster master);

    /** 获取日历概览（某月有计划的日期） */
    List<LocalDate> getCalendarDates(String userId, String yearMonth);

    /** 获取日历摘要（含过期/待完成日期） */
    java.util.Map<String, Object> getCalendarSummary(String userId, String yearMonth);

    /** 获取指定日期的计划实例 */
    List<PlanInstance> getInstancesByDate(String userId, LocalDate date);

    /** 切换实例完成状态 */
    void toggleInstanceStatus(String instanceId);

    /** 根据 ID 查询计划实例 */
    PlanInstance getInstanceById(String instanceId);

    /** 创建重复计划的后续实例 */
    PlanInstance createInstance(String masterId, LocalDate planDate);

    /** 管理端：计划完成率统计（按用户聚合） */
    java.util.List<java.util.Map<String, Object>> getCompletionStats(String userId, String yearMonth);

    /** 管理端：日历摘要（实例维度，含过期/待完成） */
    java.util.Map<String, Object> getAdminCalendarSummary(String yearMonth, String userId);

    /** 管理端：某日计划实例列表 */
    List<PlanInstance> getAdminInstancesByDate(LocalDate date, String userId);

    /** 手动补跑重复计划实例（masterId 为空则全量） */
    Map<String, Object> rollForwardRepeatInstances(String masterId);

    /** 为重复计划补齐实例（至 targetEnd  inclusive），返回新建数量 */
    int ensureRepeatInstances(PlanMaster master, LocalDate targetEnd);

    /** 批量生成重复计划实例 */
    int generateRepeatInstances(PlanMaster master, LocalDate from, LocalDate to);
}
