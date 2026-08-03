package org.jeecg.modules.homeai.plan.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.homeai.plan.entity.PlanMaster;

import java.util.Collection;

public interface PlanMasterMapper extends BaseMapper<PlanMaster> {
    //update-begin---author:admin ---date:2026-07-31  for：修复回收站功能（自定义SQL绕开逻辑删除生成的 del_flag=0 条件）-----------
    /**
     * 从回收站恢复（原生SQL，不经过 MyBatis-Plus 逻辑删除）
     */
    @Update("UPDATE homeai_plan_master SET del_flag = 0 WHERE id = #{id}")
    int restoreById(@Param("id") String id);

    /**
     * 回收站分页列表（原生SQL，避免逻辑删除自动追加 del_flag=0 导致查不到数据）
     */
    @Select({
            "<script>",
            "SELECT * FROM homeai_plan_master",
            "WHERE del_flag = 1",
            "<if test='title != null and title != \"\"'>AND title LIKE CONCAT('%', #{title}, '%')</if>",
            "<if test='category != null and category != \"\"'>AND category LIKE CONCAT('%', #{category}, '%')</if>",
            "ORDER BY create_time DESC",
            "</script>"
    })
    IPage<PlanMaster> selectRecycleBinPage(Page<PlanMaster> page, @Param("title") String title, @Param("category") String category);

    /**
     * 彻底删除（物理删除，逻辑删除下 removeByIds 对回收站数据无效）
     */
    @Delete({
            "<script>",
            "DELETE FROM homeai_plan_master WHERE id IN",
            "<foreach collection='ids' item='item' open='(' separator=',' close=')'>#{item}</foreach>",
            "</script>"
    })
    int deletePermanentlyByIds(@Param("ids") Collection<String> ids);
    //update-end---author:admin ---date:2026-07-31  for：修复回收站功能-----------
}