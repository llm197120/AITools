package org.jeecg.modules.homeai.plan.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.homeai.plan.entity.PlanInstance;

import java.util.List;
import java.util.Map;

public interface PlanInstanceMapper extends BaseMapper<PlanInstance> {

    //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R32】按完成计划统计做过次数-----------
    @Select({
            "<script>",
            "SELECT m.recipe_id AS recipeId, COUNT(*) AS cookCount",
            "FROM homeai_plan_instance i",
            "INNER JOIN homeai_plan_master m ON i.master_id = m.id",
            "WHERE i.status = 'completed'",
            "AND IFNULL(m.del_flag, 0) = 0",
            "AND m.recipe_id IS NOT NULL AND m.recipe_id &lt;&gt; ''",
            "<choose>",
            "<when test='userIds != null and userIds.size() &gt; 0'>",
            "AND m.user_id IN",
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>",
            "</when>",
            "<otherwise>AND 1 = 0</otherwise>",
            "</choose>",
            "GROUP BY m.recipe_id",
            "</script>"
    })
    List<Map<String, Object>> countCompletedByRecipe(@Param("userIds") List<String> userIds);
    //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R32】按完成计划统计做过次数-----------
}