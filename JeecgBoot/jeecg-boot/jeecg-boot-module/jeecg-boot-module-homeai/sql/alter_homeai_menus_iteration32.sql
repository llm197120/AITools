-- -*- coding: utf-8 -*-
-- 第 32 轮：家庭配额运营看板菜单
-- 执行后请刷新菜单缓存，并为角色勾选（本脚本已给 admin / vue3 授权）

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_menu_storage_quota', 'homeai_menu_storage', '家庭配额看板',
       '/homeai/storage/familyQuota', '/views/homeai/storage/familyQuota',
       1, 'homeai:storage:file:list', 1, 'ant-design:cloud-server-outlined', 6.0, 1, 0, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_storage_quota');

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`)
SELECT REPLACE(UUID(), '-', ''), r.id, 'homeai_menu_storage_quota'
FROM `sys_role` r
WHERE r.role_code = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission` rp
      WHERE rp.role_id = r.id AND rp.permission_id = 'homeai_menu_storage_quota'
  );

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`)
SELECT REPLACE(UUID(), '-', ''), r.id, 'homeai_menu_storage_quota'
FROM `sys_role` r
WHERE r.role_code = 'vue3'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission` rp
      WHERE rp.role_id = r.id AND rp.permission_id = 'homeai_menu_storage_quota'
  );
