package org.jeecg.modules.homeai.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.ai.entity.AiKeyConfig;

/**
 * AI密钥配置 Service
 */
public interface IAiKeyConfigService extends IService<AiKeyConfig> {

    /**
     * 获取启用的默认模型
     */
    AiKeyConfig getDefaultModel();

    /**
     * 获取第一个启用的模型
     */
    AiKeyConfig getFirstEnabled();

    /**
     * 加密明文 API Key（AES-256-GCM，返回 IV+密文 的 Base64）
     */
    String encryptApiKey(String rawApiKey) throws Exception;

    /**
     * 加密保存 API Key
     */
    boolean saveWithEncryption(AiKeyConfig config, String rawApiKey);

    /**
     * 解密获取 API Key
     */
    String decryptApiKey(String id);
}
