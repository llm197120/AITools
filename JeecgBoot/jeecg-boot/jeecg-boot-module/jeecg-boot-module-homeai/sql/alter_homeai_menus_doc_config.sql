-- 协议/隐私富文本配置菜单：挂在「APP版本」菜单下
UPDATE `sys_permission` SET `is_leaf` = 0 WHERE `id` = 'homeai_menu_app_version';

INSERT INTO `sys_permission`
  (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `menu_type`, `perms`, `is_leaf`, `icon`, `sort_no`, `del_flag`, `status`, `create_time`)
SELECT
  'homeai_menu_doc_config', 'homeai_menu_app_version', '协议与隐私配置',
  '/homeai/docConfig', '/views/homeai/config/docConfig',
  1, 1, 'homeai:app:version:edit', 1, 'ant-design:file-text-outlined',
  10.0, 0, 1, NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM `sys_permission` p WHERE p.`id` = 'homeai_menu_app_version')
  AND NOT EXISTS (SELECT 1 FROM `sys_permission` x WHERE x.`id` = 'homeai_menu_doc_config');

UPDATE `sys_permission`
SET `parent_id` = 'homeai_menu_app_version'
WHERE `id` = 'homeai_menu_doc_config'
  AND EXISTS (SELECT 1 FROM (SELECT `id` FROM `sys_permission` WHERE `id` = 'homeai_menu_app_version') t);

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`)
SELECT REPLACE(UUID(), '-', ''), rp.`role_id`, 'homeai_menu_doc_config'
FROM `sys_role_permission` rp
WHERE rp.`permission_id` = 'homeai_menu_app_version'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission` x
      WHERE x.`role_id` = rp.`role_id` AND x.`permission_id` = 'homeai_menu_doc_config'
  );
