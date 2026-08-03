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
    private Date startTime;
    private Date endTime;
    private Integer duration;
    private String mode;
    private String notes;
    private String createBy;
    private Date createTime;
}
