package org.jeecg.modules.homeai.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.app.service.IAiragChatService;
import org.jeecg.modules.airag.app.vo.ChatSendParams;
import org.jeecg.modules.homeai.ai.entity.AiConversation;
import org.jeecg.modules.homeai.ai.entity.AiKeyConfig;
import org.jeecg.modules.homeai.ai.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 家庭AI对话服务实现
 * 核心逻辑：复用 airag 的 SSE 流式能力，包装自有对话管理和 Token 配额
 */
@Slf4j
@Service
public class HomeaiChatServiceImpl implements IHomeaiChatService {

    /** SSE 超时时间（毫秒） */
    private static final long SSE_TIMEOUT_MS = 300000L;

    /** 存储每个对话当前 emitter，用于停止生成 */
    private final Map<String, SseEmitter> conversationEmitters = new ConcurrentHashMap<>();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private IAiragChatService airagChatService;

    @Autowired
    private IAiConversationService conversationService;

    @Autowired
    private IAiMessageService messageService;

    @Autowired
    private IAiKeyConfigService keyConfigService;

    @Autowired
    private IAiQuotaService quotaService;

    @Override
    public SseEmitter sendMessage(String userId, String conversationId, String content, List<String> images, List<String> files) {
        // 1. 获取或创建对话
        AiConversation conversation;
        boolean isNew = false;
        String modelName;
        if (conversationId == null || conversationId.isEmpty()) {
            AiKeyConfig defaultModel = keyConfigService.getDefaultModel();
            if (defaultModel == null) {
                defaultModel = keyConfigService.getFirstEnabled();
            }
            modelName = defaultModel != null ? defaultModel.getModelName() : "deepseek-chat";
            conversation = conversationService.createConversation(userId, truncateTitle(content), modelName);
            isNew = true;
        } else {
            conversation = conversationService.getById(conversationId);
            if (conversation == null) {
                throw new RuntimeException("对话不存在");
            }
            modelName = conversation.getModelName();
        }

        // 2. Token 配额预检
        int estimatedInput = content.length() / 2;
        Map<String, Object> quotaCheck = quotaService.checkQuota(userId, estimatedInput, 500);
        if (!Boolean.TRUE.equals(quotaCheck.get("allowed"))) {
            throw new RuntimeException((String) quotaCheck.get("message"));
        }

        // 3. 保存用户消息
        String msgType = (!images.isEmpty()) ? "image" : (!files.isEmpty()) ? "file" : "text";
        String msgFileUrl = (!files.isEmpty()) ? files.get(0) : (!images.isEmpty() ? images.get(0) : null);
        messageService.saveUserMessage(conversation.getId(), content, msgType, msgFileUrl);

        // 4. 调用 airag 的 SSE 流式接口
        ChatSendParams params = new ChatSendParams(content, conversation.getId(), null,
                "homeai_app");
        params.setImages(images);

        // 用自建 emitter 接收 airag 流式输出，以便捕获 AI 回复内容并保存
        HomeaiCapturingSseEmitter emitter = new HomeaiCapturingSseEmitter(SSE_TIMEOUT_MS);
        conversationEmitters.put(conversation.getId(), emitter);

        // 7. SSE 生命周期回调：完成/超时/出错时保存AI回复并扣减Token
        final String convId = conversation.getId();
        final String finalModelName = modelName;
        final int finalEstimatedInput = estimatedInput;

        emitter.onCompletion(() -> {
            try {
                String aiReply = emitter.getCapturedContent();
                if (aiReply != null && !aiReply.isEmpty()) {
                    messageService.saveAssistantMessage(convId, aiReply, null);
                    int outputTokens = aiReply.length() / 2;
                    quotaService.deductQuota(userId, convId, finalModelName,
                            finalEstimatedInput, outputTokens);
                    // 统计 AI 回复消息数
                    AiConversation conv = conversationService.getById(convId);
                    if (conv != null) {
                        conv.setMessageCount((conv.getMessageCount() != null ? conv.getMessageCount() : 0) + 1);
                        conversationService.updateById(conv);
                    }
                    log.info("AI回复已保存: convId={}, replyLen={}", convId, aiReply.length());
                }
            } catch (Exception e) {
                log.error("保存AI回复失败: convId={}", convId, e);
            } finally {
                conversationEmitters.remove(convId);
            }
        });

        emitter.onTimeout(() -> {
            log.warn("SSE 超时: convId={}", convId);
            conversationEmitters.remove(convId);
        });

        emitter.onError(ex -> {
            log.error("SSE 错误: convId={}", convId, ex);
            conversationEmitters.remove(convId);
        });

        // 4. 调用 airag 的 SSE 流式接口（将自建 emitter 交给 airag 写入）
        airagChatService.send(params, emitter);

        // 统计用户消息数
        conversation.setMessageCount((conversation.getMessageCount() != null ? conversation.getMessageCount() : 0) + 1);
        conversationService.updateById(conversation);

        return emitter;
    }

    @Override
    public void stopGeneration(String conversationId) {
        SseEmitter emitter = conversationEmitters.remove(conversationId);
        if (emitter != null) {
            // 直接结束 SSE 流，客户端立即停止接收；底层 LLM 输出由 airag 自行处理
            emitter.complete();
        }
    }

    private String truncateTitle(String content) {
        if (content == null || content.isEmpty()) {
            return "新对话";
        }
        String clean = content.replaceAll("[\\n\\r]+", " ");
        return clean.length() > 30 ? clean.substring(0, 30) + "..." : clean;
    }

    /**
     * SSE 捕获器：自身即为返回给客户端的 emitter，同时捕获流经的 AI 回复内容
     */
    static class HomeaiCapturingSseEmitter extends SseEmitter {
        private final StringBuilder capturedBuilder = new StringBuilder();

        HomeaiCapturingSseEmitter(Long timeout) {
            super(timeout);
        }

        @Override
        public void send(Object object) throws IOException {
            captureObject(object);
            super.send(object);
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            super.send(builder);
        }

        private void captureObject(Object obj) {
            if (obj == null) return;
            try {
                String data;
                if (obj instanceof byte[] bytes) {
                    data = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    data = obj.toString();
                }
                if (data == null || data.isEmpty()) return;

                // 解析 SSE data: 行，提取 JSON 中的 content 字段
                for (String line : data.split("\n")) {
                    line = line.trim();
                    if (!line.startsWith("data:")) continue;
                    String jsonPart = line.substring(5).trim();
                    if ("[DONE]".equals(jsonPart)) continue;

                    try {
                        Map<String, Object> jsonMap = objectMapper.readValue(jsonPart,
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                        Object content = jsonMap.get("content");
                        if (content instanceof String && !((String) content).isEmpty()) {
                            capturedBuilder.append((String) content);
                        }
                        // 也处理 choices[0].delta.content 格式 (OpenAI 兼容)
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) jsonMap.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                            if (delta != null) {
                                Object deltaContent = delta.get("content");
                                if (deltaContent instanceof String && !((String) deltaContent).isEmpty()) {
                                    capturedBuilder.append((String) deltaContent);
                                }
                            }
                        }
                    } catch (Exception ignored) {
                        // 非 JSON 格式数据，忽略
                    }
                }
            } catch (Exception ignored) {
                // 捕获失败不影响主流程
            }
        }

        String getCapturedContent() {
            return capturedBuilder.toString();
        }
    }
}
