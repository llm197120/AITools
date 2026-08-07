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
 * 文件记录
 */
@Data
@TableName("homeai_storage_file")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "文件记录")
public class StorageFile implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private String id;

    @Schema(description = "家庭ID")
    private String familyId;

    @Schema(description = "上传者用户ID")
    private String userId;

    @Schema(description = "所属文件夹ID")
    private String folderId;

    @Schema(description = "用户上传时的原始文件名（页面展示、重命名均使用此字段）")
    private String originalName;

    @Schema(description = "OSS/磁盘实际存储文件名（UUID+扩展名，与 originalName 分离）")
    private String storedName;

    @Schema(description = "文件扩展名")
    private String extension;

    @Schema(description = "MIME类型")
    private String mimeType;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件存储引用（oss:objectKey 或本地绝对URL，不含展示文件名）")
    private String fileUrl;

    @Schema(description = "缩略图URL")
    private String thumbnailUrl;

    @Schema(description = "可见性:private/family/public")
    private String visibility;

    @Schema(description = "是否收藏:1=是 0=否")
    private String isFavorite;

    @Schema(description = "下载次数")
    private Integer downloadCount;

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

    @Schema(description = "删除时间")
    private Date deletedAt;

    /** 家庭可见时关联的家庭 ID 列表（非数据库字段） */
    @TableField(exist = false)
    private List<String> familyIds;
}
