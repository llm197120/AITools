package org.jeecg.modules.homeai.recipe.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("homeai_recipe")
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜谱")
public class Recipe implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String familyId;
    private String userId;
    private String name;
    @TableField("category")
    private String categoryId;
    @TableField("cover_image")
    private String coverUrl;
    private String videoUrl;
    private Integer difficulty;
    private Integer cookTime;
    private Integer servings;
    @TableField(exist = false)
    private String ingredients;
    @TableField(exist = false)
    private String steps;
    private String tips;
    private String visibility;
    private Integer viewCount;
    private Integer favoriteCount;
    private String auditStatus;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    @TableLogic
    private Integer delFlag;
}
