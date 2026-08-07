package org.jeecg.modules.homeai.config.service;

/**
 * 微信小程序订阅消息
 */
public interface IHomeaiWxSubscribeService {

    /** 发送计划提醒订阅消息；未配置模板时记录日志并返回 true（便于联调） */
    boolean sendPlanRemind(String openid, String planTitle, String planTimeText);
}
