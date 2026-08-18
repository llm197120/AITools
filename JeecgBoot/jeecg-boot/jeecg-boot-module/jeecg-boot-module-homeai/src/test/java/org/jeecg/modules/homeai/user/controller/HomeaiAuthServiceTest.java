package org.jeecg.modules.homeai.user.controller;

import org.jeecg.common.util.PasswordUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 手机号密码认证（Android 迁移）纯逻辑回归测试
 *
 * <p>覆盖 HomeaiAuthController.register / loginByPassword 中新增的纯逻辑：
 * 手机号格式校验、PasswordUtil 密码 encrypt/decrypt 往返、login_type 补齐判定。
 * 无 Spring 上下文、无 Mockito，仅依赖 JUnit 5 与静态工具类。</p>
 *
 * <p>注意：PasswordUtil.decrypt 内部用平台默认字符集还原字节（源码注释：中文加密在
 * windows/linux 结果不同），本机默认 GBK 时中文往返会乱码，跑测需以 UTF-8 启动
 * surefire 分叉 JVM（-DargLine="-Dfile.encoding=UTF-8"）。</p>
 */
class HomeaiAuthServiceTest {

    /** 与 HomeaiAuthController.register/loginByPassword 完全一致的正则 */
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /** 固定 8 位 ASCII 盐（模拟注册时 UUID 截取的 per-user 盐） */
    private static final String SALT_A = "12345678";
    private static final String SALT_B = "87654321";

    // ---------- 1. 手机号校验 ----------

    @Test
    void validPhonePasses() {
        // 合法 11 位手机号：1 开头 + 第二位 3-9
        assertTrue("13800138000".matches(PHONE_REGEX));
    }

    @Test
    void invalidPhonesRejected() {
        // 非 1 开头
        assertFalse("123456".matches(PHONE_REGEX));
        // 第二位 0-2（不在 3-9 区间）
        assertFalse("10012345678".matches(PHONE_REGEX));
        // 10 位（位数不足）
        assertFalse("1380013800".matches(PHONE_REGEX));
        // 12 位（位数超长）
        assertFalse("138001380001".matches(PHONE_REGEX));
        // 空串
        assertFalse("".matches(PHONE_REGEX));
        // 含字母
        assertFalse("13800a8000".matches(PHONE_REGEX));
    }

    // ---------- 2. 密码 encrypt/decrypt 往返 ----------

    @Test
    void passwordEncryptDecryptRoundTrip() {
        String plain = "abc123456";
        String cipher = PasswordUtil.encrypt(plain, PasswordUtil.SALT, SALT_A);
        assertEquals(plain, PasswordUtil.decrypt(cipher, PasswordUtil.SALT, SALT_A));
    }

    @Test
    void chinesePasswordRoundTrip() {
        // 中文密码往返（PBEWithMD5AndDES，encrypt 显式 UTF-8 编码）
        String plain = "密码123abc";
        String cipher = PasswordUtil.encrypt(plain, PasswordUtil.SALT, SALT_A);
        assertEquals(plain, PasswordUtil.decrypt(cipher, PasswordUtil.SALT, SALT_A));
    }

    @Test
    void samePlaintextSameSaltDeterministic() {
        // 相同明文 + 相同 salt 两次 encrypt 结果一致（确定性）
        String cipher1 = PasswordUtil.encrypt("abc123456", PasswordUtil.SALT, SALT_A);
        String cipher2 = PasswordUtil.encrypt("abc123456", PasswordUtil.SALT, SALT_A);
        assertEquals(cipher1, cipher2);
    }

    @Test
    void differentSaltProducesDifferentCipher() {
        // 不同 salt 加密结果不同（per-user 盐生效）
        String cipherA = PasswordUtil.encrypt("abc123456", PasswordUtil.SALT, SALT_A);
        String cipherB = PasswordUtil.encrypt("abc123456", PasswordUtil.SALT, SALT_B);
        assertNotEquals(cipherA, cipherB);
    }

    // ---------- 3. login_type 判定 ----------

    @Test
    void emptyLoginTypeFilledWithPhone() {
        // 微信老用户未绑定手机：loginType 为空（null/空串）→ 置为 phone
        WxUser nullType = new WxUser();
        nullType.setLoginType(null);
        if (oConvertUtils.isEmpty(nullType.getLoginType())) {
            nullType.setLoginType("phone");
        }
        assertEquals("phone", nullType.getLoginType());

        WxUser emptyType = new WxUser();
        emptyType.setLoginType("");
        if (oConvertUtils.isEmpty(emptyType.getLoginType())) {
            emptyType.setLoginType("phone");
        }
        assertEquals("phone", emptyType.getLoginType());
    }

    @Test
    void existingLoginTypeUnchanged() {
        // 已为 phone / wx 时保持不变
        WxUser phone = new WxUser();
        phone.setLoginType("phone");
        if (oConvertUtils.isEmpty(phone.getLoginType())) {
            phone.setLoginType("phone");
        }
        assertEquals("phone", phone.getLoginType());

        WxUser wx = new WxUser();
        wx.setLoginType("wx");
        if (oConvertUtils.isEmpty(wx.getLoginType())) {
            wx.setLoginType("phone");
        }
        assertEquals("wx", wx.getLoginType());
    }
}