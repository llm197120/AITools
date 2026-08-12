-- 修复：综合统计菜单 + 授权给管理员角色
-- 执行后请：重启或刷新菜单缓存（退出重登 / 清 Redis sys:cache:*）

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_menu_dashboard', 'homeai_menu_root', '综合统计',
       '/homeai/dashboard/crossStats', '/views/homeai/dashboard/crossStats',
       1, 'homeai:dashboard:view', 1, 'ant-design:dashboard-outlined', 2.5, 1, 0, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_dashboard');

-- 管理员角色（role_code=admin）授权综合统计
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`)
SELECT REPLACE(UUID(), '-', ''), r.id, 'homeai_menu_dashboard'
FROM `sys_role` r
WHERE r.role_code = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission` rp
      WHERE rp.role_id = r.id AND rp.permission_id = 'homeai_menu_dashboard'
  );

-- Vue3 全角色一并授权（若存在）
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`)
SELECT REPLACE(UUID(), '-', ''), r.id, 'homeai_menu_dashboard'
FROM `sys_role` r
WHERE r.role_code = 'vue3'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission` rp
      WHERE rp.role_id = r.id AND rp.permission_id = 'homeai_menu_dashboard'
  );
