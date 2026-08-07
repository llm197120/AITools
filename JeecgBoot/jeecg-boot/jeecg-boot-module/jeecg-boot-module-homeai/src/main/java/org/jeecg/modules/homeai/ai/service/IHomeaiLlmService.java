package org.jeecg.modules.homeai.ai.service;

import org.jeecg.modules.homeai.ai.vo.HomeaiLlmResult;

/**
 * HomeAI 同步大模型调用（文档生成等非 SSE 场景）
 */
public interface IHomeaiLlmService {

    /**
     * 根据用户指令生成文档正文，并扣减 Token 配额
     *
     * @param userId      用户 ID（配额归属）
     * @param instruction 用户生成指令
     * @return 模型输出；失败或未配置模型时返回 null
     */
    HomeaiLlmResult generateDocumentContent(String userId, String instruction);
}
