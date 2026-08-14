package org.jeecg.modules.homeai.recipe.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecgframework.poi.excel.annotation.Excel;

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
    @Excel(name = "菜谱名称", width = 20)
    private String name;
    @Excel(name = "分类ID", width = 20)
    @TableField("category")
    private String categoryId;
    //update-begin---author:cursor ---date:2026-08-13 for：【菜谱导入】封面图片地址列，支持 http/https 或本地上传相对地址-----------
    @Excel(name = "封面图片地址", width = 30)
    //update-end---author:cursor ---date:2026-08-13 for：【菜谱导入】封面图片地址列-----------
    @TableField("cover_image")
    private String coverUrl;
    private String videoUrl;
    @Excel(name = "难度(1-5)", width = 12)
    private Integer difficulty;
    @Excel(name = "用时(分钟)", width = 12)
    private Integer cookTime;
    @Excel(name = "份数", width = 10)
    private Integer servings;
    /**
     * Excel 导入/导出用：食材文本（不落库）
     * 格式：名称|数量|单位;名称|数量|单位
     * 例：鸡蛋|2|个;面粉|100|克
     */
    @Excel(name = "食材(名称|数量|单位;...)", width = 40)
    @TableField(exist = false)
    private String ingredients;
    /**
     * Excel 导入/导出用：步骤文本（不落库）
     * 格式：步骤1;步骤2;步骤3（用分号或换行分隔）
     */
    @Excel(name = "步骤(分号分隔)", width = 50)
    @TableField(exist = false)
    private String steps;
    @Excel(name = "小贴士", width = 30)
    private String tips;
    @Excel(name = "可见性(private/family/public)", width = 18)
    @Schema(description = "可见性:private/family/public")
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
