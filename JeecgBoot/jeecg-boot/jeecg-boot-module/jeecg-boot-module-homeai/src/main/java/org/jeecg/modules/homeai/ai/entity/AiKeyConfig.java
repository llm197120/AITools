package org.jeecg.modules.homeai.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecg.common.system.base.entity.JeecgEntity;

import java.io.Serializable;

/**
 * AI密钥配置
 */
@Data
@TableName("homeai_ai_key_config")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "AI密钥配置")
public class AiKeyConfig extends JeecgEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 提供商:DeepSeek/Qwen/OpenAI/Anthropic/Ollama */
    @Schema(description = "提供商")
    private String provider;

    /** 模型名 */
    @Schema(description = "模型名")
    private String modelName;

    /** AES加密后的API Key */
    @Schema(description = "API Key(加密存储)")
    private String apiKeyEncrypted;

    /** API地址（NULL=默认官方地址） */
    @Schema(description = "API地址")
    private String apiBaseUrl;

    /** 备注说明 */
    @Schema(description = "备注")
    private String remark;

    /** 是否启用:1=启用 0=停用 */
    @Schema(description = "是否启用")
    private String isEnabled;

    /** 是否为默认模型:1=默认 0=否 */
    @Schema(description = "是否默认模型")
    private String isDefault;

    /** 排序号 */
    @Schema(description = "排序号")
    private Integer sortOrder;
}
