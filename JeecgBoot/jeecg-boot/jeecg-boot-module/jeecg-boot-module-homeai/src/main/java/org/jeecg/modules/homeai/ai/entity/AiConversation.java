package org.jeecg.modules.homeai.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecg.common.system.base.entity.JeecgEntity;

import java.io.Serializable;
import java.util.Date;

/**
 * AI对话主表
 */
@Data
@TableName("homeai_ai_conversation")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "AI对话主表")
public class AiConversation extends JeecgEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @Schema(description = "用户ID")
    private String userId;

    /** 对话标题（自动取首句） */
    @Schema(description = "对话标题")
    private String title;

    /** 使用的模型名 */
    @Schema(description = "使用的模型名")
    private String modelName;

    /** 消息数量 */
    @Schema(description = "消息数量")
    private Integer messageCount;

    //update-begin---author:cursor---date:2026-08-22---for:【审查D】对话软删用手写 wrapper，不加 TableLogic---
    /**
     * 删除状态(0-正常,1-已删除)。不用 {@code @TableLogic}：与 deletedAt 一起由
     * {@code AiConversationServiceImpl} 手写软删 wrapper，避免与 JeecgEntity 字段语义打架。
     */
    //update-end---author:cursor---date:2026-08-22---for:【审查D】对话软删用手写 wrapper，不加 TableLogic---
    @Schema(description = "删除状态")
    private Integer delFlag;

    /** 删除时间 */
    @Schema(description = "删除时间")
    private Date deletedAt;
}
