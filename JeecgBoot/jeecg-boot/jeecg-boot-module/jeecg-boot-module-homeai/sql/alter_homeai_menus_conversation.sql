-- =====================================================
-- HomeAI: 新增「AI对话管理」菜单（挂在 AI管理 下）
-- 执行后到 系统管理-菜单管理 重新同步/缓存权限，或重启后端
-- =====================================================
INSERT INTO sys_permission
  (id, parent_id, name, url, component, is_route, perms, is_leaf, icon, sort_no, del_flag, status, create_time)
VALUES
  ('homeai_menu_ai_conversation', 'homeai_menu_ai', 'AI对话管理',
   '/homeai/ai/conversationList', '/views/homeai/ai/conversationList',
   1, 'homeai:ai:conversation:list', 1, 'ant-design:message-outlined',
   0.5, 0, 1, NOW());
