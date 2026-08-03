package org.jeecg.modules.homeai.family.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.family.entity.FamilyMember;

import java.util.List;

/**
 * 家庭成员 Service
 */
public interface IFamilyMemberService extends IService<FamilyMember> {

    /**
     * 根据用户ID查询家庭成员关系
     * @param userId 用户ID
     * @return 家庭成员
     */
    FamilyMember getByUserId(String userId);

    /**
     * 查询家庭的所有成员
     * @param familyId 家庭ID
     * @return 成员列表
     */
    List<FamilyMember> getByFamilyId(String familyId);

    /**
     * 统计家庭成员数量
     * @param familyId 家庭ID
     * @return 数量
     */
    long countByFamilyId(String familyId);

    /**
     * 设置用户所属家庭关联（管理端）
     * 一个用户只能属于一个家庭（表唯一约束 user_id）
     * @param userId   用户ID
     * @param familyId 家庭ID（null 或空表示解除关联）
     * @param role     家庭成员角色（默认 member）
     * @return 是否成功
     */
    boolean setUserFamily(String userId, String familyId, String role);

    /**
     * 解除用户家庭关联（返回原关联家庭ID，便于刷新成员数）
     * @param userId 用户ID
     * @return 原家庭ID（可能为 null）
     */
    String removeUserFamily(String userId);
}
