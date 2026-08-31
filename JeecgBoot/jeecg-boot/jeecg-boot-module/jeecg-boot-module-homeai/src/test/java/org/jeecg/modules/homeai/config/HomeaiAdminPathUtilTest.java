package org.jeecg.modules.homeai.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 管理端路径：APP 写接口不得误判；控制台列表/配置必须拦。
 */
class HomeaiAdminPathUtilTest {

    @Test
    void appPathsStayOpen() {
        assertFalse(HomeaiAdminPathUtil.isAdminPath("/homeai/storage/folders", "GET"));
        assertFalse(HomeaiAdminPathUtil.isAdminPath("/homeai/storage/my/usage", "GET"));
        assertFalse(HomeaiAdminPathUtil.isAdminPath("/homeai/bill/entry", "GET"));
        assertFalse(HomeaiAdminPathUtil.isAdminPath("/homeai/bill/entry", "PUT"));
        assertFalse(HomeaiAdminPathUtil.isAdminPath("/homeai/bill/category", "PUT"));
        assertFalse(HomeaiAdminPathUtil.isAdminPath("/homeai/plan/instance/abc", "GET"));
        assertFalse(HomeaiAdminPathUtil.isAdminPath("/homeai/plan/instance/abc", "PUT"));
        assertFalse(HomeaiAdminPathUtil.isAdminPath("/homeai/ai/quota/precheck", "GET"));
        assertFalse(HomeaiAdminPathUtil.isAdminPath("/homeai/user/info", "GET"));
        assertFalse(HomeaiAdminPathUtil.isAdminPath("/homeai/storage/rule/targets", "GET"));
        assertFalse(HomeaiAdminPathUtil.isAdminPath("/homeai/storage/template/enabled", "GET"));
        assertFalse(HomeaiAdminPathUtil.isAdminPath("/homeai/recipe/category/all", "GET"));
        assertFalse(HomeaiAdminPathUtil.isAdminPath("/homeai/learn/category/all", "GET"));
        assertFalse(HomeaiAdminPathUtil.isAdminPath("/homeai/recipe/rid/video", "PUT"));
        assertFalse(HomeaiAdminPathUtil.isAdminPath("/homeai/app/version", "GET"));
    }

    @Test
    void consolePathsRequireAdmin() {
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/bill/list", "GET"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/storage/folder-list", "GET"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/storage/file-list", "GET"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/recipe/add", "POST"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/recipe/abc", "PUT"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/learn/material/mid", "PUT"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/family/fid", "PUT"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/user", "POST"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/user/list", "GET"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/config/storage", "GET"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/config/storage", "PUT"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/config/storage/family/fid", "GET"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/config/storage/families", "GET"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/config/file-whitelist", "PUT"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/storage/rule", "POST"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/recipe/category", "POST"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/app/version/admin", "GET"));
        assertTrue(HomeaiAdminPathUtil.isAdminPath("/homeai/app/version/upload", "POST"));
    }

    @Test
    void emptyPathIsNotAdmin() {
        assertFalse(HomeaiAdminPathUtil.isAdminPath(null, "GET"));
        assertFalse(HomeaiAdminPathUtil.isAdminPath("", "GET"));
    }
}
