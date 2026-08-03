package org.jeecg.modules.homeai.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.plan.entity.PlanMaster;
import org.jeecg.modules.homeai.plan.entity.PlanInstance;
import org.jeecg.modules.homeai.plan.mapper.PlanMasterMapper;
import org.jeecg.modules.homeai.plan.mapper.PlanInstanceMapper;
import org.jeecg.modules.homeai.plan.service.IPlanService;
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

    @Override
    public PlanMaster createPlan(PlanMaster master) {
        master.setCreateTime(new Date());
        save(master);
        createInstance(master.getId(), master.getPlanDate() != null ? master.getPlanDate() : LocalDate.now());
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
        LocalDate start = LocalDate.parse(yearMonth + "-01");
        LocalDate end = start.plusMonths(1);
        LambdaQueryWrapper<PlanMaster> qm = new LambdaQueryWrapper<>();
        qm.eq(PlanMaster::getUserId, userId).eq(PlanMaster::getDelFlag, 0);
        List<String> masterIds = list(qm).stream().map(PlanMaster::getId).collect(Collectors.toList());
        if (masterIds.isEmpty()) return Collections.emptyList();
        LambdaQueryWrapper<PlanInstance> qi = new LambdaQueryWrapper<>();
        qi.in(PlanInstance::getMasterId, masterIds)
          .between(PlanInstance::getPlanDate, start, end)
          .select(PlanInstance::getPlanDate);
        return instanceMapper.selectList(qi).stream().map(PlanInstance::getPlanDate).distinct().collect(Collectors.toList());
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
        // 填充主计划信息
        for (PlanInstance inst : instances) {
            PlanMaster m = getById(inst.getMasterId());
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
}
