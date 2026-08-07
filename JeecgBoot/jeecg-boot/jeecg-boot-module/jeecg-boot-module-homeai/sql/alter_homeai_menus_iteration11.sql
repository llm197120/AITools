-- 第 11 轮迭代：操作审计菜单（已有库执行）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_menu_plan_audit', 'homeai_menu_plan', '操作审计', '/homeai/plan/auditLog', '/views/homeai/plan/auditLog', 1, 'homeai:plan:list', 1, 'ant-design:audit-outlined', 4.0, 1, 0, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_plan_audit');
