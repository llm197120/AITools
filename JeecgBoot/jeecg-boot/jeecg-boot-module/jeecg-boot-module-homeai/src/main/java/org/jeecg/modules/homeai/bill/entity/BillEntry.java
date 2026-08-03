package org.jeecg.modules.homeai.bill.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
@TableName("homeai_bill_entry")
@EqualsAndHashCode(callSuper = false)
@Schema(description = "账单记录")
public class BillEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String familyId;
    private String userId;
    private LocalDate billDate;
    private String type;
    private String categoryId;
    private BigDecimal amount;
    private String paymentMethod;
    private String remark;
    private String voucherUrl;
    private String source;
    @Version
    private Integer version;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    @TableLogic
    private Integer delFlag;
}
