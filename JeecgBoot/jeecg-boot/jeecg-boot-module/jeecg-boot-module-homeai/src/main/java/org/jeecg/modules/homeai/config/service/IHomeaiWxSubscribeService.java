package org.jeecg.modules.homeai.config.service;

/**
 * 微信小程序订阅消息
 */
public interface IHomeaiWxSubscribeService {

    /** 发送计划提醒订阅消息；未配置模板时记录日志并返回 true（便于联调） */
    boolean sendPlanRemind(String openid, String planTitle, String planTimeText);

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】学习提醒-----------
    /** 发送学习目标提醒；未配置模板时记录日志并返回 true */
    boolean sendLearnRemind(String openid, int goalMinutes, int todayMinutes);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29】学习提醒-----------

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R31】学习提醒模板描述-----------
    /** 返回当前字段映射与样例 data，便于与微信后台模板联调 */
    java.util.Map<String, Object> describeLearnRemindTemplate();
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R31】学习提醒模板描述-----------
}
