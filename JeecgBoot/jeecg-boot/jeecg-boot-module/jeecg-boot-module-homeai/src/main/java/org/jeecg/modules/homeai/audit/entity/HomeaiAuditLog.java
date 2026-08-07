package org.jeecg.modules.homeai.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("homeai_audit_log")
@EqualsAndHashCode(callSuper = false)
@Schema(description = "操作审计日志")
public class HomeaiAuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String userId;
    private String actionType;
    private String module;
    private String targetId;
    private String targetSummary;
    private String detail;
    private String result;
    private String ipAddress;
    private String createBy;
    private Date createTime;
}
