package org.jeecg.modules.homeai.audit.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.audit.entity.HomeaiAuditLog;
import org.jeecg.modules.homeai.audit.mapper.HomeaiAuditLogMapper;
import org.jeecg.modules.homeai.audit.service.IHomeaiAuditLogService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class HomeaiAuditLogServiceImpl extends ServiceImpl<HomeaiAuditLogMapper, HomeaiAuditLog>
        implements IHomeaiAuditLogService {

    @Override
    public void record(String userId, String actionType, String module, String targetId,
                       String targetSummary, Map<String, Object> detail, String result, String ipAddress) {
        HomeaiAuditLog log = new HomeaiAuditLog();
        log.setUserId(userId != null ? userId : "system");
        log.setActionType(actionType);
        log.setModule(module);
        log.setTargetId(targetId);
        log.setTargetSummary(truncate(targetSummary, 500));
        if (detail != null && !detail.isEmpty()) {
            log.setDetail(JSON.toJSONString(detail));
        }
        log.setResult(oConvertUtils.isEmpty(result) ? "success" : result);
        log.setIpAddress(ipAddress);
        log.setCreateBy(userId);
        log.setCreateTime(new Date());
        save(log);
    }

    @Override
    public IPage<HomeaiAuditLog> pageByAction(String actionType, int pageNo, int pageSize) {
        LambdaQueryWrapper<HomeaiAuditLog> q = new LambdaQueryWrapper<>();
        q.eq(HomeaiAuditLog::getActionType, actionType)
                .orderByDesc(HomeaiAuditLog::getCreateTime);
        return page(new Page<>(pageNo, pageSize), q);
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
