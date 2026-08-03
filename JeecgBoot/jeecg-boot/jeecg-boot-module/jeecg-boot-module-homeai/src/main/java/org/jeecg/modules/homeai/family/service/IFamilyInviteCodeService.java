package org.jeecg.modules.homeai.family.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.family.entity.FamilyInviteCode;

/**
 * 邀请码 Service
 */
public interface IFamilyInviteCodeService extends IService<FamilyInviteCode> {

    /**
     * 生成6位邀请码
     * @param familyId 家庭ID
     * @param userId   生成人
     * @return 邀请码
     */
    FamilyInviteCode generateCode(String familyId, String userId);

    /**
     * 校验邀请码是否有效
     * @param code 6位邀请码
     * @return 邀请码记录（无效返回 null）
     */
    FamilyInviteCode validateCode(String code);
}
