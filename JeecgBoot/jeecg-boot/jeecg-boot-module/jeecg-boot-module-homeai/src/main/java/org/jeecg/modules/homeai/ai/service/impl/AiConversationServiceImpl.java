package org.jeecg.modules.homeai.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.ai.entity.AiConversation;
import org.jeecg.modules.homeai.ai.mapper.AiConversationMapper;
import org.jeecg.modules.homeai.ai.service.IAiConversationService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation>
        implements IAiConversationService {

    @Override
    public List<AiConversation> getUserConversations(String userId) {
        LambdaQueryWrapper<AiConversation> query = new LambdaQueryWrapper<>();
        query.eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getDelFlag, 0)
                .orderByDesc(AiConversation::getUpdateTime);
        return list(query);
    }

    @Override
    public AiConversation createConversation(String userId, String title, String modelName) {
        AiConversation conv = new AiConversation();
        conv.setUserId(userId);
        conv.setTitle(title != null ? title : "新对话");
        conv.setModelName(modelName);
        conv.setMessageCount(0);
        conv.setDelFlag(0);
        conv.setCreateTime(new Date());
        conv.setUpdateTime(new Date());
        save(conv);
        return conv;
    }

    @Override
    public void renameConversation(String id, String title) {
        AiConversation conv = getById(id);
        if (conv != null) {
            conv.setTitle(title);
            conv.setUpdateTime(new Date());
            updateById(conv);
        }
    }

    @Override
    public void softDelete(String id) {
        // @TableLogic 字段不参与 updateById，需通过 update wrapper 显式设置
        update(new LambdaUpdateWrapper<AiConversation>()
                .eq(AiConversation::getId, id)
                .set(AiConversation::getDelFlag, 1)
                .set(AiConversation::getDeletedAt, new Date()));
    }
}
