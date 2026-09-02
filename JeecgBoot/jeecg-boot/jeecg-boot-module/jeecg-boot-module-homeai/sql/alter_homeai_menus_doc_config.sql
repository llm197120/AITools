-- 协议/隐私富文本配置菜单：挂在「APP版本」菜单（homeai:app:version:edit）下
INSERT INTO `sys_permission`
  (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `menu_type`, `perms`, `is_leaf`, `icon`, `sort_no`, `del_flag`, `status`, `create_time`)
SELECT
  'homeai_menu_doc_config', sp.`id`, '协议与隐私配置',
  '/homeai/docConfig', '/views/homeai/config/docConfig',
  1, 1, 'homeai:app:version:edit', 1, 'ant-design:file-text-outlined',
  10.0, 0, 1, NOW()
FROM `sys_permission` sp
WHERE sp.`perms` = 'homeai:app:version:edit'
  AND NOT EXISTS (SELECT 1 FROM `sys_permission` x WHERE x.`id` = 'homeai_menu_doc_config')
LIMIT 1;