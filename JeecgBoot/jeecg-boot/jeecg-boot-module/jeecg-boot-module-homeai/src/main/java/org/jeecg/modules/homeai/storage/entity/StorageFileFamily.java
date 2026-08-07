package org.jeecg.modules.homeai.storage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("homeai_storage_file_family")
@Schema(description = "文件-家庭可见关联")
public class StorageFileFamily implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String fileId;

    private String familyId;

    private Date createTime;
}
