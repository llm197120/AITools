package org.jeecg.modules.homeai.recipe.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("homeai_recipe_step")
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜谱步骤")
public class RecipeStep implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String recipeId;
    @TableField("step_number")
    private Integer stepNum;
    private String description;
    private String imageUrl;
    private Integer sortOrder;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
