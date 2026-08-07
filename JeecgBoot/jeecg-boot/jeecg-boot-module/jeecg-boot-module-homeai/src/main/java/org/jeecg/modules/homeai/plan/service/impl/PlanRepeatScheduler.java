package org.jeecg.modules.homeai.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.plan.entity.PlanMaster;
import org.jeecg.modules.homeai.plan.mapper.PlanMasterMapper;
import org.jeecg.modules.homeai.plan.service.IPlanService;
import org.jeecg.modules.homeai.config.service.IHomeaiPlanConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 重复计划滚动生成：每日补齐未来第 90 天实例
 */
@Slf4j
@Service
public class PlanRepeatScheduler {

    @Autowired
    private PlanMasterMapper masterMapper;

    @Autowired
    private IPlanService planService;

    @Autowired
    private IHomeaiPlanConfigService planConfigService;

    @Scheduled(cron = "0 10 0 * * ?")
    public void rollForwardRepeatInstances() {
        LambdaQueryWrapper<PlanMaster> q = new LambdaQueryWrapper<>();
        q.eq(PlanMaster::getDelFlag, 0).eq(PlanMaster::getIsRepeatMaster, 1);
        List<PlanMaster> masters = masterMapper.selectList(q);
        if (masters.isEmpty()) {
            return;
        }
        LocalDate horizon = LocalDate.now().plusDays(planConfigService.getRepeatHorizonDays());
        int created = 0;
        for (PlanMaster master : masters) {
            created += planService.ensureRepeatInstances(master, horizon);
        }
        if (created > 0) {
            log.info("重复计划滚动生成完成: {} 条新实例", created);
        }
    }
}
