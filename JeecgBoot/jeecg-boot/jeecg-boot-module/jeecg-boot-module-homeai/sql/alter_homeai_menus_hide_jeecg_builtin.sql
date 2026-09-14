-- 隐藏 Jeecg 内置演示/低代码相关顶级菜单及其全部子菜单（含按钮权限）
-- 幂等：重复执行仍为 hidden=1。不改 del_flag，系统管理里可再打开。
-- 不匹配「家庭AI」下同名项（如账单「统计报表」），仅处理顶级（无父级）节点。
-- 执行后请退出重登或清 Redis 菜单缓存。

UPDATE `sys_permission` sp
INNER JOIN (
  WITH RECURSIVE tree AS (
    SELECT `id`
    FROM `sys_permission`
    WHERE `del_flag` = 0
      AND (`parent_id` IS NULL OR `parent_id` = '' OR `parent_id` = '0')
      AND `name` IN (
        '主页',
        '低代码开发',
        '数据可视化',
        '我的租户',
        '系统监控',
        '消息中心',
        '统计报表',
        '组件示例',
        '导航示例'
      )
    UNION ALL
    SELECT p.`id`
    FROM `sys_permission` p
    INNER JOIN tree t ON p.`parent_id` = t.`id`
    WHERE p.`del_flag` = 0
  )
  SELECT `id` FROM tree
) hide_ids ON sp.`id` = hide_ids.`id`
SET sp.`hidden` = 1;
