package org.jeecg.modules.homeai.learn.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.service.IHomeaiSysConfigService;
import org.jeecg.modules.homeai.config.service.IHomeaiWxSubscribeService;
import org.jeecg.modules.homeai.config.util.HomeaiLearnRemindCron;
import org.jeecg.modules.homeai.recipe.service.ILearnService;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.service.IWxUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 学习每日目标提醒（晚上推送未达标用户）
 */
@Slf4j
@Component
public class LearnRemindScheduler {

    private static final String REMINDED_KEY = "homeai:learn:reminded:";

    @Autowired
    private IHomeaiSysConfigService sysConfigService;

    @Autowired
    private ILearnService learnService;

    @Autowired
    private IWxUserService wxUserService;

    @Autowired
    private IHomeaiWxSubscribeService wxSubscribeService;

    @Autowired
    private RedisUtil redisUtil;

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】学习提醒定时任务-----------
    /** 每分钟对齐后台配置的 cron（默认每天 20:00） */
    //update-begin---author:cursor---date:2026-09-07---for:【系统配置】学习提醒 cron 后台可改立即生效-----------
    @Scheduled(cron = "0 * * * * ?")
    public void sendLearnReminders() {
        if (sysConfigService == null || !sysConfigService.isLearnRemindEnabled()) {
            return;
        }
        if (!HomeaiLearnRemindCron.matchesMinute(sysConfigService.getLearnRemindCron(), LocalDateTime.now())) {
            return;
        }
    //update-end---author:cursor---date:2026-09-07---for:【系统配置】学习提醒 cron 后台可改立即生效-----------
        String today = LocalDate.now().toString();
        List<WxUser> users = wxUserService.list();
        if (users == null || users.isEmpty()) {
            return;
        }
        int sent = 0;
        for (WxUser user : users) {
            if (user == null || oConvertUtils.isEmpty(user.getId()) || oConvertUtils.isEmpty(user.getOpenid())) {
                continue;
            }
            String dedupeKey = REMINDED_KEY + user.getId() + ":" + today;
            if (redisUtil.get(dedupeKey) != null) {
                continue;
            }
            try {
                Map<String, Object> progress = learnService.getTodayProgress(user.getId());
                boolean reached = Boolean.TRUE.equals(progress.get("reached"));
                if (reached) {
                    continue;
                }
                int goal = ((Number) progress.getOrDefault("goalMinutes", 30)).intValue();
                int todayMinutes = ((Number) progress.getOrDefault("todayMinutes", 0)).intValue();
                boolean ok = wxSubscribeService.sendLearnRemind(user.getOpenid(), goal, todayMinutes);
                if (ok) {
                    redisUtil.set(dedupeKey, "1", 86400);
                    sent++;
                }
            } catch (Exception e) {
                log.warn("学习提醒处理失败 userId={}", user.getId(), e);
            }
        }
        if (sent > 0) {
            log.info("学习提醒已发送 {} 条", sent);
        }
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29】学习提醒定时任务-----------
}
