package org.jeecg.modules.homeai.recipe.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("homeai_learn_material")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "学习资料")
public class LearnMaterial implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String userId;
    private String title;
    private String type;
    private String fileUrl;
    private String coverUrl;
    //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R63】Office 预览 PDF 缓存-----------
    @TableField("preview_pdf_url")
    private String previewPdfUrl;
    //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R63】Office 预览 PDF 缓存-----------
    private String category;
    @TableField("category_id")
    private String categoryId;
    /** 标签（逗号分隔） */
    private String tags;
    private String description;
    private Integer totalDuration;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    @TableLogic private Integer delFlag;
}
