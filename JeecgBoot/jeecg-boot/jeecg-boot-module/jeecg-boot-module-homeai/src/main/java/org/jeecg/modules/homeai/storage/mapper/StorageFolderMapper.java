package org.jeecg.modules.homeai.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.homeai.storage.entity.StorageFolder;

import java.util.Collection;

public interface StorageFolderMapper extends BaseMapper<StorageFolder> {

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】文件夹回收站-----------
    @Update("UPDATE homeai_storage_folder SET del_flag = 0, deleted_at = NULL WHERE id = #{id}")
    int restoreById(@Param("id") String id);

    @Select({
            "<script>",
            "SELECT * FROM homeai_storage_folder",
            "WHERE del_flag = 1",
            "<if test='keyword != null and keyword != \"\"'>AND name LIKE CONCAT('%', #{keyword}, '%')</if>",
            "ORDER BY deleted_at DESC, create_time DESC",
            "</script>"
    })
    IPage<StorageFolder> selectRecycleBinPage(Page<StorageFolder> page, @Param("keyword") String keyword);

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R24】用户侧文件夹回收站-----------
    @Select({
            "<script>",
            "SELECT * FROM homeai_storage_folder",
            "WHERE del_flag = 1 AND user_id = #{userId}",
            "<if test='keyword != null and keyword != \"\"'>AND name LIKE CONCAT('%', #{keyword}, '%')</if>",
            "ORDER BY deleted_at DESC, create_time DESC",
            "</script>"
    })
    IPage<StorageFolder> selectMyRecycleBinPage(Page<StorageFolder> page,
                                                @Param("userId") String userId,
                                                @Param("keyword") String keyword);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R24】用户侧文件夹回收站-----------

    @Delete({
            "<script>",
            "DELETE FROM homeai_storage_folder WHERE id IN",
            "<foreach collection='ids' item='item' open='(' separator=',' close=')'>#{item}</foreach>",
            "</script>"
    })
    int deletePermanentlyByIds(@Param("ids") Collection<String> ids);

    @Select("SELECT * FROM homeai_storage_folder WHERE parent_id = #{parentId} AND del_flag = 1")
    java.util.List<StorageFolder> selectDeletedChildren(@Param("parentId") String parentId);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】文件夹回收站-----------
}
