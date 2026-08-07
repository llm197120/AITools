package org.jeecg.modules.homeai.ai.service.impl;



import com.alibaba.fastjson.JSONObject;

import dev.langchain4j.data.message.ChatMessage;

import dev.langchain4j.data.message.UserMessage;

import dev.langchain4j.service.TokenStream;

import lombok.extern.slf4j.Slf4j;

import org.jeecg.ai.factory.AiModelFactory;

import org.jeecg.ai.handler.AIParams;

import org.jeecg.ai.handler.LLMHandler;

import org.jeecg.common.util.oConvertUtils;

import org.jeecg.modules.airag.llm.handler.AIChatHandler;

import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;

import org.jeecg.modules.homeai.ai.entity.AiConversation;

import org.jeecg.modules.homeai.ai.entity.AiKeyConfig;

import org.jeecg.modules.homeai.ai.entity.AiMessage;

import org.jeecg.modules.homeai.ai.service.*;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;



import java.io.IOException;

import java.util.ArrayList;

import java.util.Collections;

import java.util.List;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;



/**

 * 家庭AI对话服务实现

 * 使用 HomeAI 密钥配置 + LLMHandler 流式输出（airag EventData SSE 协议）

 */

@Slf4j

@Service

public class HomeaiChatServiceImpl implements IHomeaiChatService {



    /** SSE 超时时间（毫秒） */

    private static final long SSE_TIMEOUT_MS = 300000L;



    /** 存储每个对话当前 emitter，用于停止生成 */

    private final Map<String, SseEmitter> conversationEmitters = new ConcurrentHashMap<>();



    @Autowired

    private LLMHandler llmHandler;



    @Autowired

    private AIChatHandler aiChatHandler;



    @Autowired

    private IAiConversationService conversationService;



    @Autowired

    private IAiMessageService messageService;



    @Autowired

    private IAiKeyConfigService keyConfigService;



    @Autowired

    private IAiQuotaService quotaService;



    @Autowired

    private IHomeaiFileStorageService fileStorageService;



    @Override

    public SseEmitter sendMessage(String userId, String conversationId, String content, List<String> images, List<String> files) {

        if (images == null) {

            images = Collections.emptyList();

        }

        if (files == null) {

            files = Collections.emptyList();

        }



        AiKeyConfig modelConfig = keyConfigService.getDefaultModel();

        if (modelConfig == null) {

            modelConfig = keyConfigService.getFirstEnabled();

        }

        if (modelConfig == null) {

            throw new RuntimeException("未配置可用的 AI 模型，请在管理端添加密钥");

        }

        String apiKey = keyConfigService.decryptApiKey(modelConfig.getId());

        if (oConvertUtils.isEmpty(apiKey)) {

            throw new RuntimeException("AI 模型 API Key 无效，请检查密钥配置");

        }



        AiConversation conversation;

        String modelName = modelConfig.getModelName();

        if (conversationId == null || conversationId.isEmpty()) {

            conversation = conversationService.createConversation(userId, truncateTitle(content), modelName);

        } else {

            conversation = conversationService.getById(conversationId);

            if (conversation == null) {

                throw new RuntimeException("对话不存在");

            }

        }



        int estimatedInput = content.length() / 2;

        Map<String, Object> quotaCheck = quotaService.checkQuota(userId, estimatedInput, 500);

        if (!Boolean.TRUE.equals(quotaCheck.get("allowed"))) {

            throw new RuntimeException((String) quotaCheck.get("message"));

        }



        String msgType = (!images.isEmpty()) ? "image" : (!files.isEmpty()) ? "file" : "text";

        String msgFileUrl = (!files.isEmpty()) ? files.get(0) : (!images.isEmpty() ? images.get(0) : null);

        messageService.saveUserMessage(conversation.getId(), content, msgType, msgFileUrl);



        final String convId = conversation.getId();

        final int finalEstimatedInput = estimatedInput;

        final String finalModelName = modelName;



        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        conversationEmitters.put(convId, emitter);

        StringBuilder replyBuilder = new StringBuilder();



        emitter.onTimeout(() -> {

            log.warn("SSE 超时: convId={}", convId);

            conversationEmitters.remove(convId);

        });

        emitter.onError(ex -> {

            log.error("SSE 错误: convId={}", convId, ex);

            conversationEmitters.remove(convId);

        });



        try {

            List<String> aiImages = new ArrayList<>();

            for (String img : images) {

                aiImages.add(fileStorageService.resolveAccessUrl(img));

            }

            List<ChatMessage> chatMessages = buildChatMessages(convId, content, aiImages);

            AIParams aiParams = buildAiParams(modelConfig, apiKey);

            aiParams.setTimeout(5 * 30 * 1000);



            TokenStream stream = llmHandler.chat(chatMessages, aiParams);

            stream.onPartialResponse(chunk -> {

                if (oConvertUtils.isEmpty(chunk)) {

                    return;

                }

                replyBuilder.append(chunk);

                try {

                    sendSseEvent(emitter, convId, "MESSAGE", chunk);

                } catch (IOException e) {

                    log.error("SSE 推送失败: convId={}", convId, e);

                }

            }).onCompleteResponse(response -> {

                try {

                    sendSseEvent(emitter, convId, "MESSAGE_END", "");

                    emitter.complete();

                } catch (IOException e) {

                    emitter.completeWithError(e);

                } finally {

                    persistAssistantReply(userId, convId, finalModelName, finalEstimatedInput, replyBuilder.toString());

                    conversationEmitters.remove(convId);

                }

            }).onError(error -> {

                log.error("LLM 流式输出失败: convId={}", convId, error);

                try {

                    String errMsg = error.getMessage() != null ? error.getMessage() : "AI 服务异常";

                    sendSseError(emitter, convId, errMsg);

                    emitter.complete();

                } catch (IOException e) {

                    emitter.completeWithError(e);

                } finally {

                    conversationEmitters.remove(convId);

                }

            }).start();

        } catch (Exception e) {

            log.error("启动 AI 对话失败: convId={}", convId, e);

            try {

                sendSseError(emitter, convId, e.getMessage());

                emitter.complete();

            } catch (IOException ex) {

                emitter.completeWithError(ex);

            }

            conversationEmitters.remove(convId);

        }



        conversation.setMessageCount((conversation.getMessageCount() != null ? conversation.getMessageCount() : 0) + 1);

        conversationService.updateById(conversation);



        return emitter;

    }



    @Override

    public void stopGeneration(String conversationId) {

        SseEmitter emitter = conversationEmitters.remove(conversationId);

        if (emitter != null) {

            emitter.complete();

        }

    }



    private List<ChatMessage> buildChatMessages(String conversationId, String latestContent, List<String> images) {

        List<AiMessage> dbMessages = messageService.getConversationMessages(conversationId);

        List<ChatMessage> chatMessages = new ArrayList<>();

        for (AiMessage msg : dbMessages) {

            if ("user".equals(msg.getRole())) {

                chatMessages.add(UserMessage.from(msg.getContent()));

            } else if ("assistant".equals(msg.getRole())) {

                chatMessages.add(dev.langchain4j.data.message.AiMessage.from(msg.getContent()));

            }

        }

        if (!images.isEmpty() && !chatMessages.isEmpty()) {

            ChatMessage last = chatMessages.get(chatMessages.size() - 1);

            if (last instanceof UserMessage) {

                chatMessages.remove(chatMessages.size() - 1);

                chatMessages.add(aiChatHandler.buildUserMessage(latestContent, images));

            }

        }

        return chatMessages;

    }



    private AIParams buildAiParams(AiKeyConfig model, String apiKey) {

        AIParams params = new AIParams();

        params.setProvider(mapProvider(model.getProvider()));

        params.setModelName(model.getModelName());

        params.setApiKey(apiKey);

        params.setBaseUrl(resolveBaseUrl(model));

        return params;

    }



    private String mapProvider(String provider) {

        if (oConvertUtils.isEmpty(provider)) {

            return AiModelFactory.AIMODEL_TYPE_OPENAI;

        }

        switch (provider.trim().toLowerCase()) {

            case "qwen":

            case "通义":

                return AiModelFactory.AIMODEL_TYPE_QWEN;

            case "anthropic":

            case "claude":

                return AiModelFactory.AIMODEL_TYPE_ANTHROPIC;

            case "ollama":

                return AiModelFactory.AIMODEL_TYPE_OLLAMA;

            case "deepseek":

            case "openai":

            default:

                return AiModelFactory.AIMODEL_TYPE_OPENAI;

        }

    }



    private String resolveBaseUrl(AiKeyConfig model) {

        if (oConvertUtils.isNotEmpty(model.getApiBaseUrl())) {

            return model.getApiBaseUrl();

        }

        String provider = model.getProvider() != null ? model.getProvider().toLowerCase() : "";

        if (provider.contains("deepseek")) {

            return "https://api.deepseek.com";

        }

        return null;

    }



    private void persistAssistantReply(String userId, String convId, String modelName,

                                       int estimatedInput, String aiReply) {

        if (oConvertUtils.isEmpty(aiReply)) {

            return;

        }

        try {

            messageService.saveAssistantMessage(convId, aiReply, null);

            int outputTokens = aiReply.length() / 2;

            quotaService.deductQuota(userId, convId, modelName, estimatedInput, outputTokens);

            AiConversation conv = conversationService.getById(convId);

            if (conv != null) {

                conv.setMessageCount((conv.getMessageCount() != null ? conv.getMessageCount() : 0) + 1);

                conversationService.updateById(conv);

            }

            log.info("AI回复已保存: convId={}, replyLen={}", convId, aiReply.length());

        } catch (Exception e) {

            log.error("保存AI回复失败: convId={}", convId, e);

        }

    }



    private void sendSseEvent(SseEmitter emitter, String conversationId, String event, String message) throws IOException {

        JSONObject data = new JSONObject();

        data.put("message", message != null ? message : "");

        JSONObject payload = new JSONObject();

        payload.put("event", event);

        payload.put("conversationId", conversationId);

        payload.put("data", data);

        emitter.send(SseEmitter.event().data(payload.toJSONString()));

    }



    private void sendSseError(SseEmitter emitter, String conversationId, String message) throws IOException {

        JSONObject data = new JSONObject();

        data.put("success", false);

        data.put("message", message != null ? message : "AI 服务异常");

        JSONObject payload = new JSONObject();

        payload.put("event", "FLOW_ERROR");

        payload.put("conversationId", conversationId);

        payload.put("data", data);

        emitter.send(SseEmitter.event().data(payload.toJSONString()));

    }



    private String truncateTitle(String content) {

        if (content == null || content.isEmpty()) {

            return "新对话";

        }

        String clean = content.replaceAll("[\\n\\r]+", " ");

        return clean.length() > 30 ? clean.substring(0, 30) + "..." : clean;

    }

}


