package org.jeecg.modules.homeai.learn.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.service.IHomeaiWxSubscribeService;
import org.jeecg.modules.homeai.recipe.service.ILearnService;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.service.IWxUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 学习每日目标提醒（晚上推送未达标用户）
 */
@Slf4j
@Component
public class LearnRemindScheduler {

    private static final String REMINDED_KEY = "homeai:learn:reminded:";

    @Value("${homeai.learn.remind-enabled:true}")
    private boolean remindEnabled;

    @Autowired
    private ILearnService learnService;

    @Autowired
    private IWxUserService wxUserService;

    @Autowired
    private IHomeaiWxSubscribeService wxSubscribeService;

    @Autowired
    private RedisUtil redisUtil;

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】学习提醒定时任务-----------
    /** 每天 20:00 检查未达每日目标的用户 */
    @Scheduled(cron = "${homeai.learn.remind-cron:0 0 20 * * ?}")
    public void sendLearnReminders() {
        if (!remindEnabled) {
            return;
        }
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
