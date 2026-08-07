package org.jeecg.modules.homeai.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.plan.entity.PlanInstance;
import org.jeecg.modules.homeai.plan.mapper.PlanInstanceMapper;
import org.jeecg.modules.homeai.config.service.IHomeaiPlanConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 清理 30 天前的计划实例（物理删除）
 */
@Slf4j
@Service
public class PlanInstanceCleanupScheduler {

    @Autowired
    private PlanInstanceMapper instanceMapper;

    @Autowired
    private IHomeaiPlanConfigService planConfigService;

    @Scheduled(cron = "0 20 1 * * ?")
    public void cleanupOldInstances() {
        LocalDate cutoff = LocalDate.now().minusDays(planConfigService.getInstanceCleanupDays());
        LambdaQueryWrapper<PlanInstance> q = new LambdaQueryWrapper<>();
        q.lt(PlanInstance::getPlanDate, cutoff);
        int removed = instanceMapper.delete(q);
        if (removed > 0) {
            log.info("计划实例清理完成: 删除 {} 条（早于 {}）", removed, cutoff);
        }
    }
}
