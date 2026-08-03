-- 学习管理：新增「学习记录」菜单
INSERT INTO `sys_permission`
  (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `is_leaf`, `icon`, `sort_no`, `del_flag`, `status`, `create_time`)
VALUES
  ('homeai_menu_learn_record', 'homeai_menu_learn_list', '学习记录',
   '/homeai/learn/learnRecord', '/views/homeai/learn/learnRecord',
   1, 'homeai:learn:material:list', 1, 'ant-design:history-outlined',
   1.5, 0, 1, NOW());
