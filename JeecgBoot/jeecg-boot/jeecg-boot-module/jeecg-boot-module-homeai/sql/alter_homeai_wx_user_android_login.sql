-- Android迁移：homeai_wx_user 增加手机号密码登录所需字段
-- password: 密码（PBE加密存储），salt: 密码盐，login_type: 登录方式（默认wechat微信登录，phone为手机号密码登录）
ALTER TABLE `homeai_wx_user`
    ADD COLUMN `password` VARCHAR(128) NULL COMMENT '密码(PBE加密)',
    ADD COLUMN `salt` VARCHAR(64) NULL COMMENT '密码盐',
    ADD COLUMN `login_type` VARCHAR(20) NULL DEFAULT 'wechat' COMMENT '登录方式:wechat/phone';
