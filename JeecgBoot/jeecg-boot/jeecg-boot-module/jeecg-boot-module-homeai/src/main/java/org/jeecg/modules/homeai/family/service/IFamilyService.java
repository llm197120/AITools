package org.jeecg.modules.homeai.family.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.family.entity.Family;
import org.jeecg.modules.homeai.family.entity.FamilyMember;

import java.util.List;

/**
 * 家庭 Service
 */
public interface IFamilyService extends IService<Family> {

    /**
     * 创建家庭
     * @param name     家庭名称
     * @param userId   创建者用户ID
     * @return 家庭
     */
    Family createFamily(String name, String userId);

    /**
     * 解散家庭
     * @param familyId 家庭ID
     * @param userId   操作人（必须是管理员）
     */
    void disband(String familyId, String userId);

    /**
     * 转让管理员
     * @param familyId      家庭ID
     * @param currentUserId 当前管理员用户ID
     * @param targetUserId  目标用户ID
     */
    void transferAdmin(String familyId, String currentUserId, String targetUserId);

    /**
     * 获取家庭详细信息
     * @param familyId 家庭ID
     * @return 家庭 + 成员列表
     */
    Family getFamilyDetail(String familyId);

    /**
     * 获取用户所属家庭
     * @param userId 用户ID
     * @return 家庭（可能为 null）
     */
    Family getByUserId(String userId);

    /**
     * 更新成员数量
     * @param familyId 家庭ID
     */
    void refreshMemberCount(String familyId);

    /**
     * 通过邀请码加入家庭（包含事务：创建成员关系 + 更新邀请码 + 刷新成员数）
     * @param userId     用户ID
     * @param familyId   家庭ID
     * @param inviteCode 邀请码ID
     */
    void joinFamily(String userId, String familyId, String inviteCodeId);
}
