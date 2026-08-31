package org.jeecg.modules.homeai.storage.util;

import org.jeecg.modules.homeai.storage.constant.StorageVisibility;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资料访问：软删不可用；APP 家庭可见只能留自己的家。
 */
class StorageAccessUtilTest {

    @Test
    void activeFileWhenNotDeleted() {
        StorageFile file = new StorageFile();
        file.setDelFlag(0);
        assertTrue(StorageAccessUtil.isActiveFile(file));
    }

    @Test
    void inactiveWhenSoftDeleted() {
        StorageFile flagged = new StorageFile();
        flagged.setDelFlag(1);
        assertFalse(StorageAccessUtil.isActiveFile(flagged));

        StorageFile stamped = new StorageFile();
        stamped.setDelFlag(0);
        stamped.setDeletedAt(new Date());
        assertFalse(StorageAccessUtil.isActiveFile(stamped));
        assertFalse(StorageAccessUtil.isActiveFile(null));
    }

    @Test
    void retainOnlyOwnFamily() {
        List<String> kept = StorageAccessUtil.retainAssignableFamilies(
                List.of("fam-other", "fam-mine", "fam-other"), "fam-mine");
        assertEquals(List.of("fam-mine"), kept);
    }

    @Test
    void retainEmptyWhenNoMembership() {
        assertTrue(StorageAccessUtil.retainAssignableFamilies(List.of("fam-x"), null).isEmpty());
        assertTrue(StorageAccessUtil.retainAssignableFamilies(List.of("fam-x"), "").isEmpty());
        assertTrue(StorageAccessUtil.retainAssignableFamilies(List.of(), "fam-mine").isEmpty());
    }

    @Test
    void ownerCanAccessAndWrite() {
        StorageFile file = new StorageFile();
        file.setUserId("u1");
        file.setVisibility(StorageVisibility.PRIVATE);
        assertTrue(StorageAccessUtil.canAccessFile("u1", "fam", file, null, null));
        assertTrue(StorageAccessUtil.canWriteFile("u1", file));
        assertFalse(StorageAccessUtil.canWriteFile("u2", file));
        assertFalse(StorageAccessUtil.canAccessFile("u2", "fam", file, null, null));
    }

    @Test
    void publicReadableNotWritableByOthers() {
        StorageFile file = new StorageFile();
        file.setUserId("u1");
        file.setVisibility(StorageVisibility.PUBLIC);
        assertTrue(StorageAccessUtil.canAccessFile("u2", "fam", file, null, null));
        assertFalse(StorageAccessUtil.canWriteFile("u2", file));
    }

    @Test
    void nullFileDenied() {
        assertFalse(StorageAccessUtil.canAccessFile("u1", "fam", null, null, null));
        assertFalse(StorageAccessUtil.canWriteFile("u1", null));
        assertFalse(StorageAccessUtil.canWriteFile(null, new StorageFile()));
    }
}
