package org.jeecg.modules.homeai.learn.util;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.homeai.recipe.entity.LearnMaterial;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 学习计时/记录必须校验资料归属，防止用他人 materialId 刷时长。
 */
class LearnMaterialAccessTest {

    private LearnMaterial material(String userId) {
        LearnMaterial m = new LearnMaterial();
        m.setId("m1");
        m.setUserId(userId);
        return m;
    }

    @Test
    void ownerCanUseOwnMaterial() {
        assertTrue(LearnMaterialAccess.canUse(material("u1"), "u1"));
    }

    @Test
    void otherUserCannotUse() {
        assertFalse(LearnMaterialAccess.canUse(material("u1"), "u2"));
    }

    @Test
    void missingMaterialCannotUse() {
        assertFalse(LearnMaterialAccess.canUse(null, "u1"));
    }

    @Test
    void emptyUserCannotUse() {
        assertFalse(LearnMaterialAccess.canUse(material("u1"), null));
        assertFalse(LearnMaterialAccess.canUse(material("u1"), ""));
    }

    @Test
    void orphanMaterialCannotUse() {
        assertFalse(LearnMaterialAccess.canUse(material(null), "u1"));
        assertFalse(LearnMaterialAccess.canUse(material(""), "u1"));
    }

    @Test
    void assertRejectsOtherUser() {
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> LearnMaterialAccess.assertCanUse(material("u1"), "u2"));
        assertEquals("无权使用该资料", ex.getMessage());
    }

    @Test
    void assertRejectsMissingMaterial() {
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> LearnMaterialAccess.assertCanUse(null, "u1"));
        assertEquals("学习资料不存在", ex.getMessage());
    }
}
