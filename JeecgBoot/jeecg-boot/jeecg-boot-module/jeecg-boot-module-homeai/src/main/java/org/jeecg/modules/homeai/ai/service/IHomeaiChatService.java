package org.jeecg.modules.homeai.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.ai.entity.AiKeyConfig;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 家庭AI对话服务
 * 复用 jeecg-boot-module-airag 的 SSE 流式能力，包装自有 Token 配额和对话管理
 */
public interface IHomeaiChatService {

    /**
     * 发送消息（SSE流式）
     * @param userId   用户ID
     * @param conversationId 对话ID（NULL=新建对话）
     * @param content  消息内容
     * @param images   图片列表（可选）
     * @return SseEmitter 流式响应
     */
    SseEmitter sendMessage(String userId, String conversationId, String content, java.util.List<String> images, java.util.List<String> files);

    /**
     * 停止生成
     * @param conversationId 对话ID
     */
    void stopGeneration(String conversationId);
}
