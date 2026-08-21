package org.jeecg.modules.homeai.learn.util;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.recipe.entity.LearnMaterial;

/**
 * 学习资料计时/记录归属校验（纯逻辑，便于单测）。
 */
public final class LearnMaterialAccess {

    private LearnMaterialAccess() {
    }

    /** APP 用户只能对自己的资料开始/结束/记时长；无主资料不可用。 */
    public static boolean canUse(LearnMaterial material, String userId) {
        if (material == null || oConvertUtils.isEmpty(userId) || oConvertUtils.isEmpty(material.getUserId())) {
            return false;
        }
        return userId.equals(material.getUserId());
    }

    public static void assertCanUse(LearnMaterial material, String userId) {
        if (material == null) {
            throw new JeecgBootException("学习资料不存在");
        }
        if (!canUse(material, userId)) {
            throw new JeecgBootException("无权使用该资料");
        }
    }
}
