package org.jeecg.modules.homeai.plan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.plan.entity.PlanMaster;
import org.jeecg.modules.homeai.plan.entity.PlanInstance;

import java.time.LocalDate;
import java.util.List;

public interface IPlanService extends IService<PlanMaster> {

    /** 创建计划（含首天实例） */
    PlanMaster createPlan(PlanMaster master);

    /** 获取日历概览（某月有计划的日期） */
    List<LocalDate> getCalendarDates(String userId, String yearMonth);

    /** 获取指定日期的计划实例 */
    List<PlanInstance> getInstancesByDate(String userId, LocalDate date);

    /** 切换实例完成状态 */
    void toggleInstanceStatus(String instanceId);

    /** 根据 ID 查询计划实例 */
    PlanInstance getInstanceById(String instanceId);

    /** 创建重复计划的后续实例 */
    PlanInstance createInstance(String masterId, LocalDate planDate);
}
