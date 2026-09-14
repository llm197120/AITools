-- 管理端默认首页改为家庭AI「综合统计」（幂等）
-- 执行后请删除 Redis 键 sys:cache:def_index::DEF_INDEX_ALL，并重启后端、退出重登。

UPDATE `sys_role_index`
SET `url` = '/homeai/dashboard/crossStats',
    `component` = 'homeai/dashboard/crossStats',
    `status` = '1'
WHERE `role_code` = 'DEF_INDEX_ALL';

-- vue3 动态表存在时同步更新
SET @has_vue3 := (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_role_index_vue3'
);
SET @sql := IF(
  @has_vue3 > 0,
  'UPDATE `sys_role_index_vue3` SET `url` = ''/homeai/dashboard/crossStats'', `component` = ''homeai/dashboard/crossStats'', `status` = ''1'' WHERE `role_code` = ''DEF_INDEX_ALL''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
