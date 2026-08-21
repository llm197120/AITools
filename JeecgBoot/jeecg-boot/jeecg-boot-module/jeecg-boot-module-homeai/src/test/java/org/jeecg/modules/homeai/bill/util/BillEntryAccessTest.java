package org.jeecg.modules.homeai.bill.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 账单删除所有权：APP 不可删他人；管理端 operator 为空可跳过。
 */
class BillEntryAccessTest {

    @Test
    void ownerCanDeleteOwnEntry() {
        assertTrue(BillEntryAccess.canSoftDelete("u1", "u1"));
    }

    @Test
    void otherUserCannotDelete() {
        assertFalse(BillEntryAccess.canSoftDelete("u2", "u1"));
    }

    @Test
    void adminRecycleBinSkipsOwnership() {
        assertTrue(BillEntryAccess.canSoftDelete(null, "u1"));
    }
}
