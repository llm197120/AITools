package org.jeecg.modules.homeai.ai.service;

import java.util.Map;

/**
 * AI 场景配额统一预检
 */
public interface IHomeaiAiQuotaPrecheckService {

    /**
     * 预检并返回完整结果（含估算与限额）
     *
     * @param userId 用户 ID
     * @param scene  场景：chat / office_generate / recipe_generate
     * @param text   本次提示词/消息（可空，空则仅查剩余）
     */
    Map<String, Object> precheck(String userId, String scene, String text);

    /**
     * 预检不通过时抛出 JeecgBootException
     */
    void assertAllowed(String userId, String scene, String text);
}
