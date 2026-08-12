-- 第 22 轮：审计独立权限 + 资料回收站按钮（已有库执行）
-- 执行后请刷新菜单缓存，并为角色勾选新权限

-- 1) 操作审计菜单权限从 homeai:plan:list 独立为 homeai:audit:list
UPDATE `sys_permission`
SET `perms` = 'homeai:audit:list'
WHERE `id` = 'homeai_menu_plan_audit'
  AND (`perms` IS NULL OR `perms` <> 'homeai:audit:list');

-- 2) 资料存储：恢复 / 彻底删除按钮权限
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_btn_storage_restore', 'homeai_menu_storage_files', '恢复', NULL, NULL, 0, 'homeai:storage:restore', 2, 2.0, 1, 0, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_btn_storage_restore');

-- id 须 ≤32（sys_permission.id）；彻底删除统一用 permDel，与其它模块一致
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_btn_storage_permDel', 'homeai_menu_storage_files', '彻底删除', NULL, NULL, 0, 'homeai:storage:deletePermanently', 2, 3.0, 1, 0, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_btn_storage_permDel');
