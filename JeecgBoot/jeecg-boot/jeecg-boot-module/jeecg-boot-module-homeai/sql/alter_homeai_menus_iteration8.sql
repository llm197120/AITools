-- 第 8 轮迭代：计划配置菜单与按钮权限（已有库执行）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_menu_plan_config', 'homeai_menu_plan', '计划配置', '/homeai/plan/planConfig', '/views/homeai/plan/planConfig', 1, 'homeai:config:plan:list', 1, 'ant-design:setting-outlined', 3.0, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_plan_config');

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_btn_plan_config_edit', 'homeai_menu_plan_config', '保存配置', NULL, NULL, 0, 'homeai:config:plan:edit', 2, 1.0, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_btn_plan_config_edit');
