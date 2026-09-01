-- APP 离线同步配置菜单：挂在「APP版本」菜单（homeai:app:version:edit）下
INSERT INTO `sys_permission`
  (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `menu_type`, `perms`, `is_leaf`, `icon`, `sort_no`, `del_flag`, `status`, `create_time`)
SELECT
  'homeai_menu_sync_config', sp.`id`, '同步配置',
  '/homeai/syncConfig', '/views/homeai/appversion/syncConfig',
  1, 1, 'homeai:app:version:edit', 1, 'ant-design:sync-outlined',
  9.0, 0, 1, NOW()
FROM `sys_permission` sp
WHERE sp.`perms` = 'homeai:app:version:edit'
  AND NOT EXISTS (SELECT 1 FROM `sys_permission` x WHERE x.`id` = 'homeai_menu_sync_config')
LIMIT 1;