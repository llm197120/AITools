package org.jeecg.modules.homeai.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.ai.entity.AiConversation;

import java.util.List;

/**
 * AI对话 Service
 */
public interface IAiConversationService extends IService<AiConversation> {

    /**
     * 获取用户对话列表（按更新时间倒序）
     */
    List<AiConversation> getUserConversations(String userId);

    /**
     * 创建新对话
     */
    AiConversation createConversation(String userId, String title, String modelName);

    /**
     * 重命名对话
     */
    void renameConversation(String id, String title);

    /**
     * 软删除对话
     */
    void softDelete(String id);
}
