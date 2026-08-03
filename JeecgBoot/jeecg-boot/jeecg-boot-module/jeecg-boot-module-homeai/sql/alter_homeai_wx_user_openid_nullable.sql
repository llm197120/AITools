-- 将 homeai_wx_user 表的 openid 字段改为可空
-- 管理端手动添加用户时 openid 可为空，等用户微信登录后自动填入
ALTER TABLE `homeai_wx_user`
    MODIFY COLUMN `openid` VARCHAR(64) NULL COMMENT '微信openid（唯一，微信登录后填入）',
    DROP INDEX `uniq_hw_user_openid`;
