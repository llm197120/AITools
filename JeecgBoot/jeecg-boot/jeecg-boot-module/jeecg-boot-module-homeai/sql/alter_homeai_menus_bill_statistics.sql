-- 账单管理：新增「统计报表」菜单
INSERT INTO `sys_permission`
  (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `is_leaf`, `icon`, `sort_no`, `del_flag`, `status`, `create_time`)
VALUES
  ('homeai_menu_bill_statistics', 'homeai_menu_bill', '统计报表',
   '/homeai/bill/billStatistics', '/views/homeai/bill/billStatistics',
   1, 'homeai:bill:list', 1, 'ant-design:bar-chart-outlined',
   3.0, 0, 1, NOW());
