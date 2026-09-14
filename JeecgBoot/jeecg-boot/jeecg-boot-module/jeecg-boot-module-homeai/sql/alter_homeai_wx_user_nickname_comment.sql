-- 用户表 nickname 列注释：微信昵称 → 用户姓名（列名不变）
ALTER TABLE `homeai_wx_user`
  MODIFY COLUMN `nickname` VARCHAR(64) NULL COMMENT '用户姓名';
