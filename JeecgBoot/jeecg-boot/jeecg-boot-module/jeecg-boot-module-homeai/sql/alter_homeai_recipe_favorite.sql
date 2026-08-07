-- 第 12 轮：菜谱用户收藏表
CREATE TABLE IF NOT EXISTS `homeai_recipe_favorite` (
    `id`          VARCHAR(32) NOT NULL COMMENT '主键',
    `user_id`     VARCHAR(32) NOT NULL COMMENT '用户ID',
    `recipe_id`   VARCHAR(32) NOT NULL COMMENT '菜谱ID',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hw_recipe_fav_user_recipe` (`user_id`, `recipe_id`),
    KEY `idx_hw_recipe_fav_recipe` (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜谱收藏' ROW_FORMAT=DYNAMIC;
