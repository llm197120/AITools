package org.jeecg.modules.homeai.bill.util;

/**
 * 账单软删所有权：APP 只能删自己的；operatorUserId 为 null 表示管理端回收站。
 */
public final class BillEntryAccess {

    private BillEntryAccess() {
    }

    public static boolean canSoftDelete(String operatorUserId, String ownerUserId) {
        return operatorUserId == null || operatorUserId.equals(ownerUserId);
    }
}
