package org.jeecg.modules.homeai.family.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.modules.homeai.family.entity.Family;
import org.jeecg.modules.homeai.family.entity.FamilyInviteCode;
import org.jeecg.modules.homeai.family.entity.FamilyMember;
import org.jeecg.modules.homeai.family.mapper.FamilyMapper;
import org.jeecg.modules.homeai.family.service.IFamilyInviteCodeService;
import org.jeecg.modules.homeai.family.service.IFamilyMemberService;
import org.jeecg.modules.homeai.family.service.IFamilyService;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.service.IWxUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 家庭 Service 实现
 */
@Slf4j
@Service
public class FamilyServiceImpl extends ServiceImpl<FamilyMapper, Family> implements IFamilyService {

    @Autowired
    private IFamilyMemberService familyMemberService;

    @Autowired
    private IFamilyInviteCodeService familyInviteCodeService;

    @Autowired
    private IWxUserService wxUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Family createFamily(String name, String userId) {
        // 1. 检查用户是否已有家庭
        Family existing = getByUserId(userId);
        if (existing != null) {
            throw new RuntimeException("用户已有家庭，不能重复创建");
        }

        // 2. 创建家庭
        Family family = new Family();
        family.setName(name);
        family.setCreatorId(userId);
        family.setMemberCount(1);
        family.setCreateTime(new Date());
        save(family);

        // 3. 将创建者设为管理员
        FamilyMember member = new FamilyMember();
        member.setFamilyId(family.getId());
        member.setUserId(userId);
        member.setRole("admin");
        member.setJoinedAt(new Date());
        member.setCreateTime(new Date());
        familyMemberService.save(member);

        log.info("家庭创建成功: familyId={}, name={}, creatorId={}", family.getId(), name, userId);
        return family;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disband(String familyId, String userId) {
        Family family = getById(familyId);
        if (family == null) {
            throw new RuntimeException("家庭不存在");
        }

        // 校验操作人是否为管理员
        FamilyMember member = familyMemberService.getByUserId(userId);
        if (member == null || !"admin".equals(member.getRole())) {
            throw new RuntimeException("仅管理员可解散家庭");
        }
        // 防止跨家庭越权：管理员只能解散自己所在家庭
        if (!familyId.equals(member.getFamilyId())) {
            throw new RuntimeException("无权解散该家庭");
        }

        // 逻辑删除家庭（@TableLogic 字段不参与 updateById，需通过 update wrapper 显式设置）
        update(new LambdaUpdateWrapper<Family>()
                .eq(Family::getId, familyId)
                .set(Family::getDelFlag, 1)
                .set(Family::getDeletedAt, new Date()));

        // 清理所有成员关系
        LambdaQueryWrapper<FamilyMember> query = new LambdaQueryWrapper<>();
        query.eq(FamilyMember::getFamilyId, familyId);
        List<FamilyMember> members = familyMemberService.list(query);
        familyMemberService.remove(query);
        // 同步所有成员的用户表缓存字段
        for (FamilyMember m : members) {
            WxUser u = wxUserService.getById(m.getUserId());
            if (u != null && familyId.equals(u.getFamilyId())) {
                u.setFamilyId(null);
                wxUserService.updateById(u);
            }
        }

        log.info("家庭已解散: familyId={}", familyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferAdmin(String familyId, String currentUserId, String targetUserId) {
        FamilyMember currentMember = familyMemberService.getByUserId(currentUserId);
        if (currentMember == null || !"admin".equals(currentMember.getRole())) {
            throw new RuntimeException("仅管理员可转让身份");
        }
        // 防止跨家庭越权：管理员只能转让自己所在家庭
        if (!familyId.equals(currentMember.getFamilyId())) {
            throw new RuntimeException("无权转让该家庭的管理员");
        }

        FamilyMember targetMember = familyMemberService.getByUserId(targetUserId);
        if (targetMember == null || !familyId.equals(targetMember.getFamilyId())) {
            throw new RuntimeException("目标用户不是家庭成员");
        }

        // 原管理员降级为普通成员
        currentMember.setRole("member");
        familyMemberService.updateById(currentMember);

        // 目标用户升级为管理员
        targetMember.setRole("admin");
        familyMemberService.updateById(targetMember);

        log.info("管理员转让成功: familyId={}, from={}, to={}", familyId, currentUserId, targetUserId);
    }

    @Override
    public Family getFamilyDetail(String familyId) {
        return getById(familyId);
    }

    @Override
    public Family getByUserId(String userId) {
        FamilyMember member = familyMemberService.getByUserId(userId);
        if (member == null || member.getFamilyId() == null) {
            return null;
        }
        return getById(member.getFamilyId());
    }

    @Override
    public void refreshMemberCount(String familyId) {
        long count = familyMemberService.countByFamilyId(familyId);
        Family family = getById(familyId);
        if (family != null) {
            family.setMemberCount((int) count);
            updateById(family);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinFamily(String userId, String familyId, String inviteCodeId) {
        FamilyMember member = new FamilyMember();
        member.setFamilyId(familyId);
        member.setUserId(userId);
        member.setRole("member");
        member.setJoinedAt(new Date());
        member.setCreateTime(new Date());
        familyMemberService.save(member);

        // 同步用户表缓存字段
        WxUser user = wxUserService.getById(userId);
        if (user != null) {
            user.setFamilyId(familyId);
            wxUserService.updateById(user);
        }

        FamilyInviteCode inviteCode = familyInviteCodeService.getById(inviteCodeId);
        if (inviteCode != null) {
            inviteCode.setUsedBy(userId);
            inviteCode.setUsedAt(new Date());
            familyInviteCodeService.updateById(inviteCode);
        }

        refreshMemberCount(familyId);
        log.info("用户加入家庭: userId={}, familyId={}", userId, familyId);
    }
}
