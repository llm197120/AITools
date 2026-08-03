package org.jeecg.modules.homeai.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.homeai.user.entity.WxUser;

import java.util.Collection;

/**
 * 微信用户 Mapper
 */
public interface WxUserMapper extends BaseMapper<WxUser> {

    //update-begin---author:admin ---date:2026-07-31  for：修复回收站功能（自定义SQL绕开逻辑删除生成的 del_flag=0 条件）-----------
    /**
     * 从回收站恢复（原生SQL，不经过 MyBatis-Plus 逻辑删除）
     */
    @Update("UPDATE homeai_wx_user SET del_flag = 0 WHERE id = #{id}")
    int restoreById(@Param("id") String id);

    /**
     * 回收站分页列表（原生SQL，避免逻辑删除自动追加 del_flag=0 导致查不到数据）
     */
    @Select({
            "<script>",
            "SELECT * FROM homeai_wx_user",
            "WHERE del_flag = 1",
            "<if test='nickname != null and nickname != \"\"'>AND nickname LIKE CONCAT('%', #{nickname}, '%')</if>",
            "<if test='phone != null and phone != \"\"'>AND phone LIKE CONCAT('%', #{phone}, '%')</if>",
            "ORDER BY create_time DESC",
            "</script>"
    })
    IPage<WxUser> selectRecycleBinPage(Page<WxUser> page, @Param("nickname") String nickname, @Param("phone") String phone);

    /**
     * 彻底删除（物理删除，逻辑删除下 removeByIds 对回收站数据无效）
     */
    @Delete({
            "<script>",
            "DELETE FROM homeai_wx_user WHERE id IN",
            "<foreach collection='ids' item='item' open='(' separator=',' close=')'>#{item}</foreach>",
            "</script>"
    })
    int deletePermanentlyByIds(@Param("ids") Collection<String> ids);
    //update-end---author:admin ---date:2026-07-31  for：修复回收站功能-----------
}
