package org.jeecg.modules.homeai.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.homeai.storage.entity.StorageFile;

import java.util.Collection;

public interface StorageFileMapper extends BaseMapper<StorageFile> {

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R22】资料文件回收站-----------
    @Update("UPDATE homeai_storage_file SET del_flag = 0, deleted_at = NULL WHERE id = #{id}")
    int restoreById(@Param("id") String id);

    @Select({
            "<script>",
            "SELECT * FROM homeai_storage_file",
            "WHERE del_flag = 1",
            "<if test='keyword != null and keyword != \"\"'>AND original_name LIKE CONCAT('%', #{keyword}, '%')</if>",
            "ORDER BY deleted_at DESC, create_time DESC",
            "</script>"
    })
    IPage<StorageFile> selectRecycleBinPage(Page<StorageFile> page, @Param("keyword") String keyword);

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R24】用户侧回收站-----------
    @Select({
            "<script>",
            "SELECT * FROM homeai_storage_file",
            "WHERE del_flag = 1 AND user_id = #{userId}",
            "<if test='keyword != null and keyword != \"\"'>AND original_name LIKE CONCAT('%', #{keyword}, '%')</if>",
            "ORDER BY deleted_at DESC, create_time DESC",
            "</script>"
    })
    IPage<StorageFile> selectMyRecycleBinPage(Page<StorageFile> page,
                                              @Param("userId") String userId,
                                              @Param("keyword") String keyword);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R24】用户侧回收站-----------

    @Delete({
            "<script>",
            "DELETE FROM homeai_storage_file WHERE id IN",
            "<foreach collection='ids' item='item' open='(' separator=',' close=')'>#{item}</foreach>",
            "</script>"
    })
    int deletePermanentlyByIds(@Param("ids") Collection<String> ids);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R22】资料文件回收站-----------
}
