package org.jeecg.modules.homeai.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.plan.entity.PlanInstance;
import org.jeecg.modules.homeai.plan.mapper.PlanInstanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Date;

/**
 * 计划实例过期标记：每日将过去未完成实例标记为 expired
 */
@Slf4j
@Service
public class PlanExpireScheduler {

    @Autowired
    private PlanInstanceMapper instanceMapper;

    @Scheduled(cron = "0 5 0 * * ?")
    public void markExpiredInstances() {
        LocalDate today = LocalDate.now();
        LambdaUpdateWrapper<PlanInstance> uw = new LambdaUpdateWrapper<>();
        uw.lt(PlanInstance::getPlanDate, today)
                .eq(PlanInstance::getStatus, "pending")
                .set(PlanInstance::getStatus, "expired")
                .set(PlanInstance::getUpdateTime, new Date());
        int count = instanceMapper.update(null, uw);
        if (count > 0) {
            log.info("计划过期标记完成: {} 条实例", count);
        }
    }
}
