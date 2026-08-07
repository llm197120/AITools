package org.jeecg.modules.homeai.storage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * Office转换历史
 */
@Data
@TableName("homeai_office_convert_history")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Office转换历史")
public class StorageConvertTask implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private String id;

    @Schema(description = "源文件ID")
    private String fileId;

    @Schema(description = "操作人")
    private String userId;

    @Schema(description = "转换类型:format_convert/ai_generate")
    private String convertType;

    @Schema(description = "源格式")
    private String sourceFormat;

    @Schema(description = "目标格式")
    private String targetFormat;

    @Schema(description = "AI生成指令")
    private String instruction;

    @Schema(description = "状态:PENDING/PROCESSING/COMPLETED/FAILED")
    private String status;

    @Schema(description = "结果文件URL")
    private String resultFileUrl;

    @Schema(description = "结果文件大小")
    private Long resultFileSize;

    @Schema(description = "失败原因")
    private String errorMessage;

    @Schema(description = "处理耗时（秒）")
    private Integer taskDuration;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "更新时间")
    private Date updateTime;

    @Schema(description = "完成时间")
    private Date completedAt;
}
