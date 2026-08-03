package org.jeecg.modules.homeai.bill.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("homeai_bill_import_record")
@EqualsAndHashCode(callSuper = false)
@Schema(description = "账单导入记录")
public class BillImportRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String userId;
    private String fileName;
    private String importType;
    private Integer totalCount;
    private Integer successCount;
    private String status;
    private String errorMessage;
    private String createBy;
    private Date createTime;
}
