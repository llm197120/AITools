-- -*- coding: utf-8 -*-
-- 第 69 轮：管理端「APP版本」菜单
-- 执行后请刷新菜单缓存（本脚本已给 admin / vue3 授权）

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_menu_app_version', 'homeai_menu_root', 'APP版本',
       '/homeai/config/appVersion', '/views/homeai/config/appVersion',
       1, 'homeai:app:version:edit', 1, 'ant-design:mobile-outlined', 2.6, 1, 0, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_app_version');

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_btn_app_version_edit', 'homeai_menu_app_version', '保存配置',
       NULL, NULL, 0, 'homeai:app:version:edit', 2, 1.0, 1, 0, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_btn_app_version_edit');

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`)
SELECT REPLACE(UUID(), '-', ''), r.id, 'homeai_menu_app_version'
FROM `sys_role` r
WHERE r.role_code = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission` rp
      WHERE rp.role_id = r.id AND rp.permission_id = 'homeai_menu_app_version'
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`)
SELECT REPLACE(UUID(), '-', ''), r.id, 'homeai_menu_app_version'
FROM `sys_role` r
WHERE r.role_code = 'vue3'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission` rp
      WHERE rp.role_id = r.id AND rp.permission_id = 'homeai_menu_app_version'
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`)
SELECT REPLACE(UUID(), '-', ''), r.id, 'homeai_btn_app_version_edit'
FROM `sys_role` r
WHERE r.role_code IN ('admin', 'vue3')
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission` rp
      WHERE rp.role_id = r.id AND rp.permission_id = 'homeai_btn_app_version_edit'
  );
