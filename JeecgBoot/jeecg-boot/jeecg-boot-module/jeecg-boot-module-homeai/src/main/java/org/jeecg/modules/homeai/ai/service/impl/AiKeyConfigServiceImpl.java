package org.jeecg.modules.homeai.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.ai.entity.AiKeyConfig;
import org.jeecg.modules.homeai.ai.mapper.AiKeyConfigMapper;
import org.jeecg.modules.homeai.ai.service.IAiKeyConfigService;
import org.jeecg.common.util.SpringContextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
public class AiKeyConfigServiceImpl extends ServiceImpl<AiKeyConfigMapper, AiKeyConfig>
        implements IAiKeyConfigService {

    /** AES-256-GCM 加密密钥（必须从外部配置注入，无默认值） */
    @Value("${homeai.ai.key-encryption-key}")
    private String encryptionKey;

    @Override
    public AiKeyConfig getDefaultModel() {
        LambdaQueryWrapper<AiKeyConfig> query = new LambdaQueryWrapper<>();
        query.eq(AiKeyConfig::getIsDefault, "1")
                .eq(AiKeyConfig::getIsEnabled, "1");
        return getOne(query);
    }

    @Override
    public AiKeyConfig getFirstEnabled() {
        LambdaQueryWrapper<AiKeyConfig> query = new LambdaQueryWrapper<>();
        query.eq(AiKeyConfig::getIsEnabled, "1")
                .orderByAsc(AiKeyConfig::getSortOrder)
                .last("LIMIT 1");
        return getOne(query);
    }

    @Override
    public String encryptApiKey(String rawApiKey) throws Exception {
        return encrypt(rawApiKey);
    }

    @Override
    public boolean saveWithEncryption(AiKeyConfig config, String rawApiKey) {
        try {
            String encrypted = encrypt(rawApiKey);
            config.setApiKeyEncrypted(encrypted);
            return save(config);
        } catch (Exception e) {
            log.error("API Key 加密失败", e);
            return false;
        }
    }

    @Override
    public String decryptApiKey(String id) {
        AiKeyConfig config = getById(id);
        if (config == null || config.getApiKeyEncrypted() == null) {
            return null;
        }
        try {
            return decrypt(config.getApiKeyEncrypted());
        } catch (Exception e) {
            log.error("API Key 解密失败", e);
            return null;
        }
    }

    /**
     * AES-256-GCM 加密
     */
    private String encrypt(String plaintext) throws Exception {
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        // 确保密钥长度为32字节（256位）
        byte[] key32 = new byte[32];
        System.arraycopy(keyBytes, 0, key32, 0, Math.min(keyBytes.length, 32));

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key32, "AES");
        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[12]; // GCM 推荐12字节IV
        secureRandom.nextBytes(iv);

        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // 返回 IV + 密文（Base64编码）
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * AES-256-GCM 解密
     */
    private String decrypt(String encryptedBase64) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        byte[] key32 = new byte[32];
        System.arraycopy(keyBytes, 0, key32, 0, Math.min(keyBytes.length, 32));

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key32, "AES");

        byte[] iv = new byte[12];
        byte[] ciphertext = new byte[combined.length - 12];
        System.arraycopy(combined, 0, iv, 0, 12);
        System.arraycopy(combined, 12, ciphertext, 0, ciphertext.length);

        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
}
