package org.jeecg.modules.homeai.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.plan.entity.PlanMaster;
import org.jeecg.modules.homeai.plan.entity.PlanInstance;
import org.jeecg.modules.homeai.plan.mapper.PlanMasterMapper;
import org.jeecg.modules.homeai.plan.mapper.PlanInstanceMapper;
import org.jeecg.modules.homeai.plan.service.IPlanService;
import org.jeecg.modules.homeai.plan.util.PlanRepeatUtil;
import org.jeecg.modules.homeai.config.service.IHomeaiPlanConfigService;
import org.jeecg.modules.homeai.recipe.entity.Recipe;
import org.jeecg.modules.homeai.recipe.service.IRecipeService;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

    @Autowired
    private IHomeaiPlanConfigService planConfigService;

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】计划关联菜谱-----------
    @Lazy
    @Autowired
    private IRecipeService recipeService;
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】计划关联菜谱-----------

    /** 计划日历缓存 key + TTL(5分钟) */
    private static final String CACHE_PLAN_CALENDAR = "homeai:cache:plan:calendar:%s:%s";
    private static final String CACHE_PLAN_CALENDAR_SUMMARY = "homeai:cache:plan:calendar:summary:%s:%s";
    private static final long CACHE_PLAN_TTL = 300;

    @Override
    public PlanMaster createPlan(PlanMaster master) {
        LocalDate start = master.getPlanDate() != null ? master.getPlanDate() : LocalDate.now();
        master.setPlanDate(start);
        if (PlanRepeatUtil.isRepeatMaster(master.getRepeatRule())) {
            master.setIsRepeatMaster(1);
        } else if (master.getIsRepeatMaster() == null) {
            master.setIsRepeatMaster(0);
        }
        master.setCreateTime(new Date());
        save(master);

        if (PlanRepeatUtil.isRepeatMaster(master.getRepeatRule())) {
            int horizon = planConfigService.getRepeatHorizonDays();
            generateRepeatInstances(master, start, start.plusDays(horizon));
        } else {
            createInstanceIfAbsent(master.getId(), start);
        }
        invalidateCalendarCache(master.getUserId(), start);
        return master;
    }

    private void createInstanceIfAbsent(String masterId, LocalDate planDate) {
        LambdaQueryWrapper<PlanInstance> q = new LambdaQueryWrapper<>();
        q.eq(PlanInstance::getMasterId, masterId).eq(PlanInstance::getPlanDate, planDate);
        if (instanceMapper.selectCount(q) > 0) {
            return;
        }
        createInstance(masterId, planDate);
    }

    @Override
    public int generateRepeatInstances(PlanMaster master, LocalDate from, LocalDate to) {
        if (master == null || from == null || to == null || from.isAfter(to)) {
            return 0;
        }
        LocalDate anchor = master.getPlanDate() != null ? master.getPlanDate() : from;
        int count = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (!PlanRepeatUtil.matchesDate(master.getRepeatRule(), anchor, d)) {
                continue;
            }
            LambdaQueryWrapper<PlanInstance> q = new LambdaQueryWrapper<>();
            q.eq(PlanInstance::getMasterId, master.getId()).eq(PlanInstance::getPlanDate, d);
            if (instanceMapper.selectCount(q) == 0) {
                createInstance(master.getId(), d);
                count++;
            }
        }
        return count;
    }

    @Override
    public int ensureRepeatInstances(PlanMaster master, LocalDate targetEnd) {
        if (master == null || !Integer.valueOf(1).equals(master.getIsRepeatMaster())) {
            return 0;
        }
        LambdaQueryWrapper<PlanInstance> q = new LambdaQueryWrapper<>();
        q.eq(PlanInstance::getMasterId, master.getId()).orderByDesc(PlanInstance::getPlanDate).last("LIMIT 1");
        PlanInstance latest = instanceMapper.selectOne(q);
        LocalDate from = latest != null && latest.getPlanDate() != null
                ? latest.getPlanDate().plusDays(1) : master.getPlanDate();
        if (from == null) {
            from = LocalDate.now();
        }
        if (from.isAfter(targetEnd)) {
            return 0;
        }
        return generateRepeatInstances(master, from, targetEnd);
    }

    private void invalidateCalendarCache(String userId, LocalDate date) {
        if (userId != null && date != null) {
            String ym = date.toString().substring(0, 7);
            redisUtil.del(String.format(CACHE_PLAN_CALENDAR, userId, ym));
            redisUtil.del(String.format(CACHE_PLAN_CALENDAR_SUMMARY, userId, ym));
        }
    }

    @Override
    public Map<String, Object> getCalendarSummary(String userId, String yearMonth) {
        String cacheKey = String.format(CACHE_PLAN_CALENDAR_SUMMARY, userId, yearMonth);
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof Map) {
            return (Map<String, Object>) cached;
        }
        LocalDate start = LocalDate.parse(yearMonth + "-01");
        LocalDate end = start.plusMonths(1).minusDays(1);
        LambdaQueryWrapper<PlanMaster> qm = new LambdaQueryWrapper<>();
        qm.eq(PlanMaster::getUserId, userId).eq(PlanMaster::getDelFlag, 0);
        List<String> masterIds = list(qm).stream().map(PlanMaster::getId).collect(Collectors.toList());
        Map<String, Object> summary = new LinkedHashMap<>();
        if (masterIds.isEmpty()) {
            summary.put("dates", Collections.emptyList());
            summary.put("expiredDates", Collections.emptyList());
            summary.put("pendingDates", Collections.emptyList());
            redisUtil.set(cacheKey, summary, CACHE_PLAN_TTL);
            return summary;
        }
        LambdaQueryWrapper<PlanInstance> qi = new LambdaQueryWrapper<>();
        qi.in(PlanInstance::getMasterId, masterIds)
                .between(PlanInstance::getPlanDate, start, end)
                .select(PlanInstance::getPlanDate, PlanInstance::getStatus);
        List<PlanInstance> instances = instanceMapper.selectList(qi);
        Set<String> allDates = new TreeSet<>();
        Set<String> expiredDates = new TreeSet<>();
        Set<String> pendingDates = new TreeSet<>();
        for (PlanInstance inst : instances) {
            if (inst.getPlanDate() == null) {
                continue;
            }
            String d = inst.getPlanDate().toString();
            allDates.add(d);
            if ("expired".equals(inst.getStatus())) {
                expiredDates.add(d);
            } else if ("pending".equals(inst.getStatus())) {
                pendingDates.add(d);
            }
        }
        summary.put("dates", new ArrayList<>(allDates));
        summary.put("expiredDates", new ArrayList<>(expiredDates));
        summary.put("pendingDates", new ArrayList<>(pendingDates));
        redisUtil.set(cacheKey, summary, CACHE_PLAN_TTL);
        return summary;
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
        fillMasterInfo(instances);
        return instances;
    }

    private void fillMasterInfo(List<PlanInstance> instances) {
        for (PlanInstance inst : instances) {
            PlanMaster m = getById(inst.getMasterId());
            if (m != null) {
                inst.setTitle(m.getTitle());
                inst.setCategory(m.getCategory());
                inst.setPriority(m.getPriority());
                inst.setIsAllDay(m.getIsAllDay());
                inst.setUserId(m.getUserId());
                inst.setRepeatRule(m.getRepeatRule());
                //update-begin---author:admin---date:2026-08-18---for:【Android迁移R2-本地通知】---
                inst.setStartTime(m.getStartTime());
                inst.setRemindMinutes(m.getRemindMinutes());
                //update-end---author:admin---date:2026-08-18---for:【Android迁移R2-本地通知】---
                //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】计划关联菜谱-----------
                inst.setRecipeId(m.getRecipeId());
                if (oConvertUtils.isNotEmpty(m.getRecipeId())) {
                    Recipe recipe = recipeService.getById(m.getRecipeId());
                    if (recipe != null) {
                        inst.setRecipeName(recipe.getName());
                    }
                }
                //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】计划关联菜谱-----------
            }
        }
    }

    @Override
    public void toggleInstanceStatus(String instanceId) {
        PlanInstance inst = instanceMapper.selectById(instanceId);
        if (inst != null) {
            if ("expired".equals(inst.getStatus())) {
                throw new org.jeecg.common.exception.JeecgBootException("已过期计划不可切换状态");
            }
            inst.setStatus("completed".equals(inst.getStatus()) ? "pending" : "completed");
            inst.setUpdateTime(new Date());
            instanceMapper.updateById(inst);
            PlanMaster master = getById(inst.getMasterId());
            if (master != null) {
                invalidateCalendarCache(master.getUserId(), inst.getPlanDate());
            }
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

    @Override
    public Map<String, Object> getAdminCalendarSummary(String yearMonth, String userId) {
        LocalDate start = LocalDate.parse(yearMonth + "-01");
        LocalDate end = start.plusMonths(1).minusDays(1);
        LambdaQueryWrapper<PlanMaster> qm = new LambdaQueryWrapper<>();
        qm.eq(PlanMaster::getDelFlag, 0);
        if (userId != null && !userId.isEmpty()) {
            qm.eq(PlanMaster::getUserId, userId);
        }
        List<String> masterIds = list(qm).stream().map(PlanMaster::getId).collect(Collectors.toList());
        Map<String, Object> summary = new LinkedHashMap<>();
        if (masterIds.isEmpty()) {
            summary.put("dates", Collections.emptyList());
            summary.put("expiredDates", Collections.emptyList());
            summary.put("pendingDates", Collections.emptyList());
            return summary;
        }
        LambdaQueryWrapper<PlanInstance> qi = new LambdaQueryWrapper<>();
        qi.in(PlanInstance::getMasterId, masterIds)
                .between(PlanInstance::getPlanDate, start, end)
                .select(PlanInstance::getPlanDate, PlanInstance::getStatus);
        List<PlanInstance> instances = instanceMapper.selectList(qi);
        Set<String> allDates = new TreeSet<>();
        Set<String> expiredDates = new TreeSet<>();
        Set<String> pendingDates = new TreeSet<>();
        for (PlanInstance inst : instances) {
            if (inst.getPlanDate() == null) {
                continue;
            }
            String d = inst.getPlanDate().toString();
            allDates.add(d);
            if ("expired".equals(inst.getStatus())) {
                expiredDates.add(d);
            } else if ("pending".equals(inst.getStatus())) {
                pendingDates.add(d);
            }
        }
        summary.put("dates", new ArrayList<>(allDates));
        summary.put("expiredDates", new ArrayList<>(expiredDates));
        summary.put("pendingDates", new ArrayList<>(pendingDates));
        return summary;
    }

    @Override
    public List<PlanInstance> getAdminInstancesByDate(LocalDate date, String userId) {
        LambdaQueryWrapper<PlanMaster> qm = new LambdaQueryWrapper<>();
        qm.eq(PlanMaster::getDelFlag, 0);
        if (userId != null && !userId.isEmpty()) {
            qm.eq(PlanMaster::getUserId, userId);
        }
        List<String> masterIds = list(qm).stream().map(PlanMaster::getId).collect(Collectors.toList());
        if (masterIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<PlanInstance> qi = new LambdaQueryWrapper<>();
        qi.in(PlanInstance::getMasterId, masterIds).eq(PlanInstance::getPlanDate, date);
        List<PlanInstance> instances = instanceMapper.selectList(qi);
        fillMasterInfo(instances);
        instances.sort(Comparator.comparing(PlanInstance::getTitle, Comparator.nullsLast(String::compareTo)));
        return instances;
    }

    @Override
    public Map<String, Object> rollForwardRepeatInstances(String masterId) {
        LocalDate targetEnd = LocalDate.now().plusDays(planConfigService.getRepeatHorizonDays());
        int created = 0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("targetEnd", targetEnd.toString());
        result.put("horizonDays", planConfigService.getRepeatHorizonDays());
        if (masterId != null && !masterId.isEmpty()) {
            PlanMaster master = getById(masterId);
            if (master == null || master.getDelFlag() != null && master.getDelFlag() != 0) {
                throw new org.jeecg.common.exception.JeecgBootException("计划不存在");
            }
            if (!Integer.valueOf(1).equals(master.getIsRepeatMaster())) {
                throw new org.jeecg.common.exception.JeecgBootException("该计划不是重复计划");
            }
            created = ensureRepeatInstances(master, targetEnd);
            if (master.getUserId() != null) {
                invalidateCalendarCache(master.getUserId(), LocalDate.now());
            }
            result.put("created", created);
            result.put("scope", "single");
            result.put("masterId", master.getId());
            result.put("masterTitle", master.getTitle());
            return result;
        }
        LambdaQueryWrapper<PlanMaster> q = new LambdaQueryWrapper<>();
        q.eq(PlanMaster::getDelFlag, 0).eq(PlanMaster::getIsRepeatMaster, 1);
        for (PlanMaster master : list(q)) {
            created += ensureRepeatInstances(master, targetEnd);
            if (master.getUserId() != null) {
                invalidateCalendarCache(master.getUserId(), LocalDate.now());
            }
        }
        result.put("created", created);
        result.put("scope", "all");
        return result;
    }
}
