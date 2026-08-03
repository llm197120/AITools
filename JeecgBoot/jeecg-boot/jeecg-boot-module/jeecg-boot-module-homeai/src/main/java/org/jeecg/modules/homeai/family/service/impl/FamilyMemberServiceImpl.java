package org.jeecg.modules.homeai.family.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.homeai.family.entity.FamilyMember;
import org.jeecg.modules.homeai.family.mapper.FamilyMemberMapper;
import org.jeecg.modules.homeai.family.service.IFamilyMemberService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 家庭成员 Service 实现
 */
@Service
public class FamilyMemberServiceImpl extends ServiceImpl<FamilyMemberMapper, FamilyMember>
        implements IFamilyMemberService {

    @Override
    public FamilyMember getByUserId(String userId) {
        LambdaQueryWrapper<FamilyMember> query = new LambdaQueryWrapper<>();
        query.eq(FamilyMember::getUserId, userId);
        return getOne(query);
    }

    @Override
    public List<FamilyMember> getByFamilyId(String familyId) {
        LambdaQueryWrapper<FamilyMember> query = new LambdaQueryWrapper<>();
        query.eq(FamilyMember::getFamilyId, familyId);
        return list(query);
    }

    @Override
    public long countByFamilyId(String familyId) {
        LambdaQueryWrapper<FamilyMember> query = new LambdaQueryWrapper<>();
        query.eq(FamilyMember::getFamilyId, familyId);
        return count(query);
    }

    @Override
    public boolean setUserFamily(String userId, String familyId, String role) {
        FamilyMember member = getByUserId(userId);
        if (member != null) {
            // 已有关联：更新家庭ID与角色
            if (familyId != null && !familyId.isEmpty()) {
                member.setFamilyId(familyId);
                if (role != null && !role.isEmpty()) {
                    member.setRole(role);
                }
                return updateById(member);
            }
            // 解除关联
            return removeById(member.getId());
        }
        // 无关联：新增（familyId 为空则无事可做）
        if (familyId == null || familyId.isEmpty()) {
            return true;
        }
        FamilyMember newMember = new FamilyMember();
        newMember.setFamilyId(familyId);
        newMember.setUserId(userId);
        newMember.setRole(role != null && !role.isEmpty() ? role : "member");
        newMember.setJoinedAt(new Date());
        newMember.setCreateTime(new Date());
        return save(newMember);
    }

    @Override
    public String removeUserFamily(String userId) {
        FamilyMember member = getByUserId(userId);
        if (member == null) {
            return null;
        }
        String oldFamilyId = member.getFamilyId();
        removeById(member.getId());
        return oldFamilyId;
    }
}
