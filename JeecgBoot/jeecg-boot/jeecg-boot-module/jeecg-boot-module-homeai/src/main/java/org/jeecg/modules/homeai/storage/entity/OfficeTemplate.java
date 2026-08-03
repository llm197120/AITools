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
 * 文档模板
 */
@Data
@TableName("homeai_office_template")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "文档模板")
public class OfficeTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private String id;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "模板类型:word/excel/ppt")
    private String type;

    @Schema(description = "模板文件URL")
    private String fileUrl;

    @Schema(description = "预览图URL")
    private String previewUrl;

    @Schema(description = "是否默认模板:1=是 0=否")
    private String isDefault;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "更新时间")
    private Date updateTime;
}
