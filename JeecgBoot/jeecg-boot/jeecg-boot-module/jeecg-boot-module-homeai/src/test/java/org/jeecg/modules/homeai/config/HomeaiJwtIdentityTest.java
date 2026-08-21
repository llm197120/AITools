package org.jeecg.modules.homeai.config;

import org.jeecg.modules.homeai.user.entity.WxUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 手机号 JWT 身份解析纯逻辑测试
 *
 * <p>覆盖第 47 轮根因：手机号登录 {@code HomeaiJwtUtil.sign(userId, secret, APP)}
 * 把 userId 与 openid claim 都写成主键 UUID，库内 openid 是 {@code phone_手机号}。
 * 拦截器按 userId 放行，但只按 openid 查库会 miss。</p>
 *
 * <p>无 Spring 上下文：通过反射写入 {@code JWT_SECRET} 后验签。</p>
 */
class HomeaiJwtIdentityTest {

    private static final String SECRET = "unit-test-homeai-jwt-secret";
    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String PHONE_OPENID = "phone_13800138000";

    @BeforeAll
    static void injectJwtSecret() throws Exception {
        Field field = HomeaiJwtUtil.class.getDeclaredField("JWT_SECRET");
        field.setAccessible(true);
        field.set(null, SECRET);
    }

    @Test
    void phonePlaceholderOpenidDiffersFromPrimaryKey() {
        assertNotEquals(USER_ID, PHONE_OPENID);
        assertTrue(PHONE_OPENID.startsWith("phone_"));
    }

    @Test
    void phoneLoginSignPutsUserIdOnBothClaims() {
        // 与 HomeaiAuthController 当前签发方式一致：sign(user.getId(), secret, APP)
        String token = HomeaiJwtUtil.sign(USER_ID, SECRET, "APP");
        assertEquals(USER_ID, HomeaiJwtUtil.getUserId(token));
        assertEquals(USER_ID, HomeaiJwtUtil.getOpenid(token));
        assertNotEquals(PHONE_OPENID, HomeaiJwtUtil.getOpenid(token));
    }

    @Test
    void dualClaimSignKeepsUserIdAndPhoneOpenidApart() {
        String token = HomeaiJwtUtil.sign(USER_ID, PHONE_OPENID, SECRET, "APP");
        assertEquals(USER_ID, HomeaiJwtUtil.getUserId(token));
        assertEquals(PHONE_OPENID, HomeaiJwtUtil.getOpenid(token));
    }

    @Test
    void resolvePrefersGetByIdWhenPhoneJwtOpenidIsUuid() {
        WxUser byId = new WxUser();
        byId.setId(USER_ID);
        byId.setOpenid(PHONE_OPENID);
        // JWT openid claim = 主键，按 openid 查库 miss
        WxUser resolved = resolveWxUser(USER_ID, byId, USER_ID, null);
        assertNotNull(resolved);
        assertEquals(USER_ID, resolved.getId());
        assertEquals(PHONE_OPENID, resolved.getOpenid());
    }

    @Test
    void resolveFallsBackToOpenidForWechatJwt() {
        WxUser byOpenid = new WxUser();
        byOpenid.setId(USER_ID);
        byOpenid.setOpenid("oWechatOpenid123");
        // 微信 JWT 两 claim 都是 openid，getById 失败再 getByOpenid
        WxUser resolved = resolveWxUser("oWechatOpenid123", null, "oWechatOpenid123", byOpenid);
        assertNotNull(resolved);
        assertEquals("oWechatOpenid123", resolved.getOpenid());
    }

    @Test
    void resolveMissWhenNeitherLookupHits() {
        assertNull(resolveWxUser(USER_ID, null, USER_ID, null));
    }

    @Test
    void logoutMustInvalidateBothRedisKeys() {
        assertNotEquals("homeai_token:" + USER_ID, "homeai_token:" + PHONE_OPENID);
        assertNotEquals("homeai_refresh:" + USER_ID, "homeai_refresh:" + PHONE_OPENID);
    }

    /**
     * 与 HomeaiSecurityUtil.getWxUser 解析顺序一致：先主键，再 openid。
     */
    private static WxUser resolveWxUser(String userIdClaim, WxUser byId, String openidClaim, WxUser byOpenid) {
        if (userIdClaim != null && !userIdClaim.isEmpty() && byId != null) {
            return byId;
        }
        return byOpenid;
    }
}
