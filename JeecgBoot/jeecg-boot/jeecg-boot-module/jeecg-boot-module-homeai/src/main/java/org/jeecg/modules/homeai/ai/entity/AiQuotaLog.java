package org.jeecg.modules.homeai.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * Token额度消耗日志
 */
@Data
@TableName("homeai_ai_quota_log")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Token额度消耗日志")
public class AiQuotaLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private String id;

    /** 用户ID */
    @Schema(description = "用户ID")
    private String userId;

    /** 对话ID */
    @Schema(description = "对话ID")
    private String conversationId;

    /** 使用的模型 */
    @Schema(description = "使用的模型")
    private String modelName;

    /** 输入Token数 */
    @Schema(description = "输入Token数")
    private Integer inputTokens;

    /** 输出Token数 */
    @Schema(description = "输出Token数")
    private Integer outputTokens;

    /** 总Token数 */
    @Schema(description = "总Token数")
    private Integer totalTokens;

    /** 扣费类型:daily=日额度 monthly=月额度 */
    @Schema(description = "扣费类型:daily=日额度 monthly=月额度")
    private String costType;

    /** 创建人 */
    @Schema(description = "创建人")
    private String createBy;

    /** 消耗时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "消耗时间")
    private Date createTime;
}
