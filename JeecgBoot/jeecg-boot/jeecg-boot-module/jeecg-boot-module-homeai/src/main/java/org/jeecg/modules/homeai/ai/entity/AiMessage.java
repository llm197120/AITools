package org.jeecg.modules.homeai.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecg.common.system.base.entity.JeecgEntity;

import java.io.Serializable;

/**
 * AI对话消息表
 */
@Data
@TableName("homeai_ai_message")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "AI对话消息")
public class AiMessage extends JeecgEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 对话ID */
    @Schema(description = "对话ID")
    private String conversationId;

    /** 角色:user/assistant/system */
    @Schema(description = "角色:user/assistant/system")
    private String role;

    /** 消息内容（AES-256-GCM加密存储） */
    @Schema(description = "消息内容")
    private String content;

    /** 内容类型:text/image/file */
    @Schema(description = "内容类型:text/image/file")
    private String contentType;

    /** 附件文件URL */
    @Schema(description = "附件文件URL")
    private String fileUrl;

    /** 本条消息消耗的Token数 */
    @Schema(description = "Token消耗数")
    private Integer tokenCount;

    /** 租户ID */
    @Schema(description = "租户ID")
    private String tenantId;

    /** 所属部门 */
    @Schema(description = "所属部门")
    private String sysOrgCode;
}
