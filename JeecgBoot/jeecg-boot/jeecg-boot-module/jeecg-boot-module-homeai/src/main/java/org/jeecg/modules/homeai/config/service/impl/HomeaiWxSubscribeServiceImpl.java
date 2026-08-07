package org.jeecg.modules.homeai.config.service.impl;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.service.IHomeaiWxSubscribeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class HomeaiWxSubscribeServiceImpl implements IHomeaiWxSubscribeService {

    private static final String ACCESS_TOKEN_KEY = "homeai:wx:access_token";

    @Value("${homeai.wechat.appid:}")
    private String appid;

    @Value("${homeai.wechat.secret:}")
    private String secret;

    @Value("${homeai.wechat.plan-remind-template-id:}")
    private String planRemindTemplateId;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public boolean sendPlanRemind(String openid, String planTitle, String planTimeText) {
        if (oConvertUtils.isEmpty(openid)) {
            return false;
        }
        if (oConvertUtils.isEmpty(planRemindTemplateId) || oConvertUtils.isEmpty(appid) || oConvertUtils.isEmpty(secret)) {
            log.info("[计划提醒-模拟推送] openid={}, title={}, time={}", openid, planTitle, planTimeText);
            return true;
        }
        try {
            String accessToken = getAccessToken();
            JSONObject body = new JSONObject();
            body.put("touser", openid);
            body.put("template_id", planRemindTemplateId);
            body.put("page", "pages-homeai-more/plan/index");
            JSONObject data = new JSONObject();
            data.put("thing1", field(planTitle));
            data.put("time2", field(planTimeText));
            body.put("data", data);

            String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken;
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject result = JSONObject.parseObject(response.body());
            if (result.getIntValue("errcode") == 0) {
                return true;
            }
            log.warn("计划提醒推送失败: openid={}, resp={}", openid, response.body());
            return false;
        } catch (Exception e) {
            log.error("计划提醒推送异常: openid={}", openid, e);
            return false;
        }
    }

    private JSONObject field(String value) {
        JSONObject o = new JSONObject();
        o.put("value", truncate(value, 20));
        return o;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String getAccessToken() throws Exception {
        Object cached = redisUtil.get(ACCESS_TOKEN_KEY);
        if (cached instanceof String && oConvertUtils.isNotEmpty((String) cached)) {
            return (String) cached;
        }
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + appid + "&secret=" + secret;
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        JSONObject json = JSONObject.parseObject(response.body());
        String token = json.getString("access_token");
        int expires = json.getIntValue("expires_in");
        if (oConvertUtils.isEmpty(token)) {
            throw new RuntimeException("获取微信 access_token 失败: " + response.body());
        }
        redisUtil.set(ACCESS_TOKEN_KEY, token, Math.max(expires - 300, 60));
        return token;
    }
}
