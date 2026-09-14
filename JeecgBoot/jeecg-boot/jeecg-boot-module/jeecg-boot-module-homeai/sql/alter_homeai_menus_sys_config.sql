-- -*- coding: utf-8 -*-
-- 系统配置菜单：挂在「家庭AI小工具」下，紧挨 APP版本
INSERT INTO `sys_permission`
  (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `menu_type`, `perms`, `is_leaf`, `icon`, `sort_no`, `del_flag`, `status`, `create_time`)
SELECT
  'homeai_menu_sys_config', 'homeai_menu_root', '系统配置',
  '/homeai/sysConfig', '/views/homeai/config/sysConfig',
  1, 1, 'homeai:config:sys:list', 1, 'ant-design:control-outlined',
  2.65, 0, 1, NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM `sys_permission` p WHERE p.`id` = 'homeai_menu_root')
  AND NOT EXISTS (SELECT 1 FROM `sys_permission` x WHERE x.`id` = 'homeai_menu_sys_config');

INSERT INTO `sys_permission`
  (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `menu_type`, `perms`, `is_leaf`, `sort_no`, `del_flag`, `status`, `create_time`)
SELECT
  'homeai_btn_sys_config_edit', 'homeai_menu_sys_config', '保存配置',
  NULL, NULL, 0, 2, 'homeai:config:sys:edit', 1, 1.0, 0, 1, NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM `sys_permission` p WHERE p.`id` = 'homeai_menu_sys_config')
  AND NOT EXISTS (SELECT 1 FROM `sys_permission` x WHERE x.`id` = 'homeai_btn_sys_config_edit');

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`)
SELECT REPLACE(UUID(), '-', ''), rp.`role_id`, 'homeai_menu_sys_config'
FROM `sys_role_permission` rp
WHERE rp.`permission_id` IN ('homeai_menu_root', 'homeai_menu_app_version')
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission` x
      WHERE x.`role_id` = rp.`role_id` AND x.`permission_id` = 'homeai_menu_sys_config'
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`)
SELECT REPLACE(UUID(), '-', ''), rp.`role_id`, 'homeai_btn_sys_config_edit'
FROM `sys_role_permission` rp
WHERE rp.`permission_id` = 'homeai_menu_sys_config'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission` x
      WHERE x.`role_id` = rp.`role_id` AND x.`permission_id` = 'homeai_btn_sys_config_edit'
  );
