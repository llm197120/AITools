package org.jeecg.modules.homeai.ai.vo;

import lombok.Data;

/**
 * 同步 LLM 调用结果（含 Token 估算）
 */
@Data
public class HomeaiLlmResult {

    private String content;
    private String modelName;
    private int inputTokens;
    private int outputTokens;

    public static HomeaiLlmResult of(String content, String modelName, int inputTokens, int outputTokens) {
        HomeaiLlmResult r = new HomeaiLlmResult();
        r.setContent(content);
        r.setModelName(modelName);
        r.setInputTokens(inputTokens);
        r.setOutputTokens(outputTokens);
        return r;
    }
}
