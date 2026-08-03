package org.jeecg.modules.homeai.storage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 文件夹
 */
@Data
@TableName("homeai_storage_folder")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "文件夹")
public class StorageFolder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private String id;

    @Schema(description = "家庭ID")
    private String familyId;

    @Schema(description = "创建者用户ID")
    private String userId;

    @Schema(description = "父文件夹ID")
    private String parentId;

    @Schema(description = "文件夹名称")
    private String name;

    @Schema(description = "可见性:private/family")
    private String visibility;

    @Schema(description = "嵌套层级")
    private Integer level;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "更新时间")
    private Date updateTime;

    @Schema(description = "删除状态")
    private Integer delFlag;

    /** 子文件夹（非数据库字段） */
    @TableField(exist = false)
    private List<StorageFolder> children;

    /** 文件数量（非数据库字段） */
    @TableField(exist = false)
    private Integer fileCount;
}
