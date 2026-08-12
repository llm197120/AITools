-- 第 26 轮：综合统计菜单（计划完成率 + 学习时长）
-- 执行后请刷新菜单缓存，并为角色勾选 homeai:dashboard:view

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_menu_dashboard', 'homeai_menu_root', '综合统计',
       '/homeai/dashboard/crossStats', '/views/homeai/dashboard/crossStats',
       1, 'homeai:dashboard:view', 1, 'ant-design:dashboard-outlined', 2.5, 1, 0, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_dashboard');
