package org.jeecg.modules.homeai.family.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.family.entity.FamilyInviteCode;
import org.jeecg.modules.homeai.family.mapper.FamilyInviteCodeMapper;
import org.jeecg.modules.homeai.family.service.IFamilyInviteCodeService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 邀请码 Service 实现
 */
@Slf4j
@Service
public class FamilyInviteCodeServiceImpl extends ServiceImpl<FamilyInviteCodeMapper, FamilyInviteCode>
        implements IFamilyInviteCodeService {

    /** 邀请码有效时长（24小时） */
    private static final long CODE_EXPIRE_MS = 24 * 60 * 60 * 1000L;

    @Override
    public FamilyInviteCode generateCode(String familyId, String userId) {
        // 生成6位字母数字验证码
        String code = generateRandomCode(6);

        FamilyInviteCode inviteCode = new FamilyInviteCode();
        inviteCode.setFamilyId(familyId);
        inviteCode.setInviteCode(code);
        inviteCode.setExpireAt(new Date(System.currentTimeMillis() + CODE_EXPIRE_MS));
        inviteCode.setCreateTime(new Date());
        save(inviteCode);

        log.info("邀请码生成: familyId={}, code={}", familyId, code);
        return inviteCode;
    }

    @Override
    public FamilyInviteCode validateCode(String code) {
        LambdaQueryWrapper<FamilyInviteCode> query = new LambdaQueryWrapper<>();
        query.eq(FamilyInviteCode::getInviteCode, code)
                .isNull(FamilyInviteCode::getUsedBy)
                .gt(FamilyInviteCode::getExpireAt, new Date())
                .orderByDesc(FamilyInviteCode::getCreateTime)
                .last("LIMIT 1");
        return getOne(query);
    }

    /**
     * 生成指定长度的随机字母数字字符串
     */
    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 排除易混淆字符 0/O/1/I
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
