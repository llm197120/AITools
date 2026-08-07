package org.jeecg.modules.homeai.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.config.service.IHomeaiWxSubscribeService;
import org.jeecg.modules.homeai.config.service.IHomeaiPlanConfigService;
import org.jeecg.modules.homeai.plan.entity.PlanInstance;
import org.jeecg.modules.homeai.plan.entity.PlanMaster;
import org.jeecg.modules.homeai.plan.mapper.PlanInstanceMapper;
import org.jeecg.modules.homeai.plan.service.IPlanService;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.mapper.WxUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * 计划提醒：每分钟扫描需推送的实例
 */
@Slf4j
@Service
public class PlanRemindScheduler {

    @Autowired
    private PlanInstanceMapper instanceMapper;

    @Autowired
    private IPlanService planService;

    @Autowired
    private WxUserMapper wxUserMapper;

    @Autowired
    private IHomeaiWxSubscribeService wxSubscribeService;

    @Autowired
    private IHomeaiPlanConfigService planConfigService;

    @Scheduled(cron = "0 * * * * ?")
    public void sendPlanReminders() {
        if (!planConfigService.isRemindEnabled()) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<PlanInstance> q = new LambdaQueryWrapper<>();
        q.eq(PlanInstance::getPlanDate, today)
                .eq(PlanInstance::getStatus, "pending")
                .and(w -> w.isNull(PlanInstance::getReminded).or().eq(PlanInstance::getReminded, 0));
        List<PlanInstance> instances = instanceMapper.selectList(q);
        if (instances.isEmpty()) {
            return;
        }

        for (PlanInstance inst : instances) {
            PlanMaster master = planService.getById(inst.getMasterId());
            if (master == null || master.getRemindMinutes() == null || master.getRemindMinutes() <= 0) {
                continue;
            }
            LocalDateTime planDateTime = resolvePlanDateTime(master, today);
            LocalDateTime remindAt = planDateTime.minusMinutes(master.getRemindMinutes());
            if (now.isBefore(remindAt)) {
                continue;
            }
            WxUser user = wxUserMapper.selectById(master.getUserId());
            if (user == null || user.getOpenid() == null) {
                continue;
            }
            String timeText = planDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            boolean ok = wxSubscribeService.sendPlanRemind(user.getOpenid(), master.getTitle(), timeText);
            if (ok) {
                inst.setReminded(1);
                inst.setUpdateTime(new Date());
                instanceMapper.updateById(inst);
            }
        }
    }

    private LocalDateTime resolvePlanDateTime(PlanMaster master, LocalDate date) {
        if (Integer.valueOf(1).equals(master.getIsAllDay()) || master.getStartTime() == null) {
            return date.atTime(9, 0);
        }
        return LocalDateTime.of(date, master.getStartTime());
    }
}
