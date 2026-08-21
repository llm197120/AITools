package org.jeecg.modules.homeai.recipe.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("homeai_learn_record")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "学习记录")
public class LearnRecord implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String userId;
    private String materialId;
    //update-begin---author:cursor---date:2026-08-20---for:【Android体验】旧库 study_date 无默认值，insert 必须写入---
    @TableField("study_date")
    @Schema(description = "学习日期")
    private Date studyDate;
    //update-end---author:cursor---date:2026-08-20---for:【Android体验】旧库 study_date 无默认值，insert 必须写入---
    private Date startTime;
    private Date endTime;
    private Integer duration;
    private String mode;
    private String notes;
    private String createBy;
    private Date createTime;
}
