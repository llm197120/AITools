package org.jeecg.modules.homeai.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.ai.entity.AiMessage;
import org.jeecg.modules.homeai.ai.mapper.AiMessageMapper;
import org.jeecg.modules.homeai.ai.service.IAiMessageService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage>
        implements IAiMessageService {

    @Override
    public List<AiMessage> getConversationMessages(String conversationId) {
        LambdaQueryWrapper<AiMessage> query = new LambdaQueryWrapper<>();
        query.eq(AiMessage::getConversationId, conversationId)
                .orderByAsc(AiMessage::getCreateTime);
        return list(query);
    }

    @Override
    public AiMessage saveUserMessage(String conversationId, String content, String contentType, String fileUrl) {
        AiMessage msg = new AiMessage();
        msg.setConversationId(conversationId);
        msg.setRole("user");
        msg.setContent(content);
        msg.setContentType(contentType != null ? contentType : "text");
        msg.setFileUrl(fileUrl);
        msg.setCreateTime(new Date());
        save(msg);
        return msg;
    }

    @Override
    public AiMessage saveAssistantMessage(String conversationId, String content, Integer tokenCount) {
        AiMessage msg = new AiMessage();
        msg.setConversationId(conversationId);
        msg.setRole("assistant");
        msg.setContent(content);
        msg.setContentType("text");
        msg.setTokenCount(tokenCount);
        msg.setCreateTime(new Date());
        save(msg);
        return msg;
    }
}
