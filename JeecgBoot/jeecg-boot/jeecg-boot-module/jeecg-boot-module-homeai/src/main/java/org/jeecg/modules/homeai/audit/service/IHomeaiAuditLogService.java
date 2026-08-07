package org.jeecg.modules.homeai.audit.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.audit.entity.HomeaiAuditLog;

import java.util.Map;

public interface IHomeaiAuditLogService extends IService<HomeaiAuditLog> {

    void record(String userId, String actionType, String module, String targetId,
                String targetSummary, Map<String, Object> detail, String result, String ipAddress);

    IPage<HomeaiAuditLog> pageByAction(String actionType, int pageNo, int pageSize);
}
