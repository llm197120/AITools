package org.jeecg.modules.homeai.ai.service.impl;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.ai.factory.AiModelFactory;
import org.jeecg.ai.handler.AIParams;
import org.jeecg.ai.handler.LLMHandler;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.ai.entity.AiKeyConfig;
import org.jeecg.modules.homeai.ai.service.IAiKeyConfigService;
import org.jeecg.modules.homeai.ai.service.IAiQuotaService;
import org.jeecg.modules.homeai.ai.service.IHomeaiLlmService;
import org.jeecg.modules.homeai.ai.util.HomeaiAiQuotaEstimateUtil;
import org.jeecg.modules.homeai.ai.vo.HomeaiLlmResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

@Slf4j
@Service
public class HomeaiLlmServiceImpl implements IHomeaiLlmService {

    private static final String DOC_SYSTEM_PROMPT =
            "你是家庭办公文档助手。根据用户指令撰写结构清晰的中文文档正文，"
                    + "可使用 Markdown 标题与列表，不要输出多余解释或前后缀。";

    private static final String QUOTA_SCENE = "storage:ai_generate";

    @Autowired
    private IAiKeyConfigService keyConfigService;

    @Autowired
    private IAiQuotaService quotaService;

    @Autowired
    private LLMHandler llmHandler;

    @Override
    public HomeaiLlmResult generateDocumentContent(String userId, String instruction) {
        if (oConvertUtils.isEmpty(instruction)) {
            return null;
        }
        AiKeyConfig model = keyConfigService.getDefaultModel();
        if (model == null) {
            model = keyConfigService.getFirstEnabled();
        }
        if (model == null) {
            log.warn("AI文档润色跳过：未配置可用模型");
            return null;
        }
        String apiKey = keyConfigService.decryptApiKey(model.getId());
        if (oConvertUtils.isEmpty(apiKey)) {
            log.warn("AI文档润色跳过：无法解密 API Key, modelId={}", model.getId());
            return null;
        }

        String trimmed = instruction.trim();
        int estimatedInput = HomeaiAiQuotaEstimateUtil.estimateInputTokens(trimmed);
        int estimatedOutput = HomeaiAiQuotaEstimateUtil.estimateOutputTokens(trimmed);
        if (oConvertUtils.isNotEmpty(userId)) {
            Map<String, Object> quotaCheck = quotaService.checkQuota(userId, estimatedInput, estimatedOutput);
            if (!Boolean.TRUE.equals(quotaCheck.get("allowed"))) {
                throw new JeecgBootException(String.valueOf(quotaCheck.get("message")));
            }
        }

        try {
            AIParams params = new AIParams();
            params.setProvider(mapProvider(model.getProvider()));
            params.setModelName(model.getModelName());
            params.setApiKey(apiKey);
            params.setBaseUrl(resolveBaseUrl(model));
            String reply = llmHandler.completions(Arrays.asList(
                    new SystemMessage(DOC_SYSTEM_PROMPT),
                    new UserMessage(trimmed)), params);
            if (oConvertUtils.isEmpty(reply)) {
                return null;
            }
            String content = reply.trim();
            int inputTokens = estimatedInput;
            int outputTokens = content.length() / 2;
            if (oConvertUtils.isNotEmpty(userId)) {
                quotaService.deductQuota(userId, QUOTA_SCENE, model.getModelName(), inputTokens, outputTokens);
            }
            return HomeaiLlmResult.of(content, model.getModelName(), inputTokens, outputTokens);
        } catch (JeecgBootException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI文档润色失败: model={}", model.getModelName(), e);
            return null;
        }
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
}
