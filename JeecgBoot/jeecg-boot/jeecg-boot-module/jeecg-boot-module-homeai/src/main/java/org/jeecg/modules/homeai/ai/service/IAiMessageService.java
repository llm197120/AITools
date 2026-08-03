package org.jeecg.modules.homeai.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.ai.entity.AiMessage;

import java.util.List;

/**
 * AI消息 Service
 */
public interface IAiMessageService extends IService<AiMessage> {

    /**
     * 获取对话消息列表
     */
    List<AiMessage> getConversationMessages(String conversationId);

    /**
     * 保存用户消息
     */
    AiMessage saveUserMessage(String conversationId, String content, String contentType, String fileUrl);

    /**
     * 保存AI回复消息
     */
    AiMessage saveAssistantMessage(String conversationId, String content, Integer tokenCount);
}
