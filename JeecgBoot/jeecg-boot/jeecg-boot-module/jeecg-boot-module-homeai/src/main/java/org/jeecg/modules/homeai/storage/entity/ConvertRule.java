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
 * 格式转换规则
 */
@Data
@TableName("homeai_convert_rule")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "格式转换规则")
public class ConvertRule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private String id;

    @Schema(description = "源格式(如docx)")
    private String sourceFormat;

    @Schema(description = "目标格式(如pdf)")
    private String targetFormat;

    @Schema(description = "是否启用:1=启用 0=停用")
    private String isEnabled;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "更新时间")
    private Date updateTime;
}
