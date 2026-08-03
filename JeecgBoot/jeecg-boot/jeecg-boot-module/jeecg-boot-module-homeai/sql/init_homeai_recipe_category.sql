-- 菜谱分类表
CREATE TABLE IF NOT EXISTS `homeai_recipe_category` (
  `id`         VARCHAR(32) NOT NULL COMMENT '主键',
  `name`       VARCHAR(50) NOT NULL COMMENT '分类名称',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `is_default` TINYINT DEFAULT 0 COMMENT '系统默认(1=不可删除)',
  `create_by`  VARCHAR(32) NULL,
  `create_time` DATETIME NULL,
  `update_by`  VARCHAR(32) NULL,
  `update_time` DATETIME NULL,
  `del_flag`   INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱分类';

-- 默认分类
INSERT INTO `homeai_recipe_category` (`id`, `name`, `sort_order`, `is_default`, `create_time`)
SELECT * FROM (
  SELECT 'rc_hot' AS id, '热菜' AS name, 1 AS sort_order, 1 AS is_default, NOW() AS ct UNION ALL
  SELECT 'rc_cold', '凉菜', 2, 1, NOW() UNION ALL
  SELECT 'rc_soup', '汤羹', 3, 1, NOW() UNION ALL
  SELECT 'rc_staple', '主食', 4, 1, NOW() UNION ALL
  SELECT 'rc_bake', '烘焙', 5, 1, NOW() UNION ALL
  SELECT 'rc_drink', '饮品', 6, 1, NOW() UNION ALL
  SELECT 'rc_snack', '小食', 7, 1, NOW() UNION ALL
  SELECT 'rc_other', '其他', 8, 1, NOW()
) t
WHERE NOT EXISTS (SELECT 1 FROM `homeai_recipe_category`);
