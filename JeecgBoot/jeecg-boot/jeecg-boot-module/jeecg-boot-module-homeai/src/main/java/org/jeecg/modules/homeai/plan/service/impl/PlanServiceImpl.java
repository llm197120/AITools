package org.jeecg.modules.homeai.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.plan.entity.PlanMaster;
import org.jeecg.modules.homeai.plan.entity.PlanInstance;
import org.jeecg.modules.homeai.plan.mapper.PlanMasterMapper;
import org.jeecg.modules.homeai.plan.mapper.PlanInstanceMapper;
import org.jeecg.modules.homeai.plan.service.IPlanService;
import org.jeecg.common.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlanServiceImpl extends ServiceImpl<PlanMasterMapper, PlanMaster> implements IPlanService {

    @Autowired
    private PlanInstanceMapper instanceMapper;

    @Autowired
    private RedisUtil redisUtil;

    /** 计划日历缓存 key + TTL(5分钟) */
    private static final String CACHE_PLAN_CALENDAR = "homeai:cache:plan:calendar:%s:%s";
    private static final long CACHE_PLAN_TTL = 300;

    @Override
    public PlanMaster createPlan(PlanMaster master) {
        master.setCreateTime(new Date());
        save(master);
        createInstance(master.getId(), master.getPlanDate() != null ? master.getPlanDate() : LocalDate.now());
        if (master.getUserId() != null && master.getPlanDate() != null) {
            redisUtil.del(String.format(CACHE_PLAN_CALENDAR, master.getUserId(),
                    master.getPlanDate().toString().substring(0, 7)));
        }
        return master;
    }

    @Override
    public PlanInstance createInstance(String masterId, LocalDate planDate) {
        PlanInstance inst = new PlanInstance();
        inst.setMasterId(masterId);
        inst.setPlanDate(planDate);
        inst.setStatus("pending");
        inst.setReminded(0);
        inst.setCreateTime(new Date());
        instanceMapper.insert(inst);
        return inst;
    }

    @Override
    public PlanInstance getInstanceById(String instanceId) {
        return instanceMapper.selectById(instanceId);
    }

    @Override
    public List<LocalDate> getCalendarDates(String userId, String yearMonth) {
        String cacheKey = String.format(CACHE_PLAN_CALENDAR, userId, yearMonth);
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            return (List<LocalDate>) cached;
        }
        LocalDate start = LocalDate.parse(yearMonth + "-01");
        LocalDate end = start.plusMonths(1);
        LambdaQueryWrapper<PlanMaster> qm = new LambdaQueryWrapper<>();
        qm.eq(PlanMaster::getUserId, userId).eq(PlanMaster::getDelFlag, 0);
        List<String> masterIds = list(qm).stream().map(PlanMaster::getId).collect(Collectors.toList());
        if (masterIds.isEmpty()) {
            redisUtil.set(cacheKey, Collections.emptyList(), CACHE_PLAN_TTL);
            return Collections.emptyList();
        }
        LambdaQueryWrapper<PlanInstance> qi = new LambdaQueryWrapper<>();
        qi.in(PlanInstance::getMasterId, masterIds)
          .between(PlanInstance::getPlanDate, start, end)
          .select(PlanInstance::getPlanDate);
        List<LocalDate> result = instanceMapper.selectList(qi).stream()
                .map(PlanInstance::getPlanDate).distinct().collect(Collectors.toList());
        redisUtil.set(cacheKey, result, CACHE_PLAN_TTL);
        return result;
    }

    @Override
    public List<PlanInstance> getInstancesByDate(String userId, LocalDate date) {
        LambdaQueryWrapper<PlanMaster> qm = new LambdaQueryWrapper<>();
        qm.eq(PlanMaster::getUserId, userId).eq(PlanMaster::getDelFlag, 0);
        List<String> masterIds = list(qm).stream().map(PlanMaster::getId).collect(Collectors.toList());
        if (masterIds.isEmpty()) return Collections.emptyList();
        LambdaQueryWrapper<PlanInstance> qi = new LambdaQueryWrapper<>();
        qi.in(PlanInstance::getMasterId, masterIds).eq(PlanInstance::getPlanDate, date);
        List<PlanInstance> instances = instanceMapper.selectList(qi);
        // 填充主计划信息（标题/分类/优先级/是否全天）
        for (PlanInstance inst : instances) {
            PlanMaster m = getById(inst.getMasterId());
            if (m != null) {
                inst.setTitle(m.getTitle());
                inst.setCategory(m.getCategory());
                inst.setPriority(m.getPriority());
                inst.setIsAllDay(m.getIsAllDay());
            }
        }
        return instances;
    }

    @Override
    public void toggleInstanceStatus(String instanceId) {
        PlanInstance inst = instanceMapper.selectById(instanceId);
        if (inst != null) {
            inst.setStatus("completed".equals(inst.getStatus()) ? "pending" : "completed");
            inst.setUpdateTime(new Date());
            instanceMapper.updateById(inst);
        }
    }

    @Override
    public List<Map<String, Object>> getCompletionStats(String userId, String yearMonth) {
        LocalDate ref = LocalDate.now();
        if (yearMonth != null && !yearMonth.isEmpty()) {
            try {
                ref = LocalDate.parse(yearMonth + "-01");
            } catch (Exception ignored) {
            }
        }
        LocalDate start = ref.withDayOfMonth(1);
        LocalDate end = ref.plusMonths(1).withDayOfMonth(1);

        LambdaQueryWrapper<PlanMaster> qm = new LambdaQueryWrapper<>();
        qm.eq(PlanMaster::getDelFlag, 0);
        if (userId != null && !userId.isEmpty()) {
            qm.eq(PlanMaster::getUserId, userId);
        }
        List<PlanMaster> masters = list(qm);
        if (masters.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> masterIds = masters.stream().map(PlanMaster::getId).collect(Collectors.toList());
        Map<String, String> masterUserMap = new HashMap<>();
        for (PlanMaster m : masters) {
            masterUserMap.put(m.getId(), m.getUserId());
        }
        LambdaQueryWrapper<PlanInstance> qi = new LambdaQueryWrapper<>();
        qi.in(PlanInstance::getMasterId, masterIds).between(PlanInstance::getPlanDate, start, end);
        List<PlanInstance> instances = instanceMapper.selectList(qi);

        Map<String, int[]> statsMap = new LinkedHashMap<>(); // userId -> [total, completed]
        for (PlanInstance inst : instances) {
            String uid = masterUserMap.get(inst.getMasterId());
            if (uid == null) continue;
            int[] arr = statsMap.computeIfAbsent(uid, k -> new int[2]);
            arr[0]++;
            if ("completed".equals(inst.getStatus())) {
                arr[1]++;
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, int[]> e : statsMap.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", e.getKey());
            row.put("total", e.getValue()[0]);
            row.put("completed", e.getValue()[1]);
            row.put("rate", e.getValue()[0] == 0 ? 0 : Math.round(e.getValue()[1] * 100.0 / e.getValue()[0]));
            result.add(row);
        }
        return result;
    }
}
