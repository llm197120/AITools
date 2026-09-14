package org.jeecg.modules.homeai.permission;

import org.jeecg.modules.system.entity.SysPermission;
import org.jeecg.modules.system.util.PermissionDataUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 隐藏顶级菜单树过滤：整棵演示树去掉，可见父级下的隐藏子路由保留。
 */
class PermissionDataUtilHiddenRootTest {

    @Test
    void dropsHiddenRootAndDescendantsButKeepsHiddenChildOfVisibleParent() {
        List<SysPermission> list = new ArrayList<>();
        list.add(menu("demo", null, 0, true));
        list.add(menu("demo-child", "demo", 1, true));
        list.add(menu("demo-btn", "demo-child", 2, true));
        list.add(menu("homeai", null, 0, false));
        list.add(menu("homeai-detail", "homeai", 1, true));

        PermissionDataUtil.removeHiddenRootTrees(list);

        List<String> ids = list.stream().map(SysPermission::getId).collect(Collectors.toList());
        assertEquals(2, ids.size());
        assertTrue(ids.contains("homeai"));
        assertTrue(ids.contains("homeai-detail"));
    }

    @Test
    void leavesVisibleTreesUntouched() {
        List<SysPermission> list = new ArrayList<>();
        list.add(menu("root", null, 0, false));
        list.add(menu("child", "root", 1, false));
        PermissionDataUtil.removeHiddenRootTrees(list);
        assertEquals(2, list.size());
    }

    private static SysPermission menu(String id, String parentId, int menuType, boolean hidden) {
        SysPermission p = new SysPermission();
        p.setId(id);
        p.setParentId(parentId);
        p.setMenuType(menuType);
        p.setHidden(hidden);
        return p;
    }
}
