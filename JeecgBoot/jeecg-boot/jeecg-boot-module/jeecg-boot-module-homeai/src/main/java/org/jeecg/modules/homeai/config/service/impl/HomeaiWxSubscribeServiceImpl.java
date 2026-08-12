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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class HomeaiWxSubscribeServiceImpl implements IHomeaiWxSubscribeService {

    private static final String ACCESS_TOKEN_KEY = "homeai:wx:access_token";
    private static final DateTimeFormatter CN_DATE = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA);

    @Value("${homeai.wechat.appid:}")
    private String appid;

    @Value("${homeai.wechat.secret:}")
    private String secret;

    @Value("${homeai.wechat.plan-remind-template-id:}")
    private String planRemindTemplateId;

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】学习提醒-----------
    @Value("${homeai.wechat.learn-remind-template-id:}")
    private String learnRemindTemplateId;
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29】学习提醒-----------

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R31】学习提醒模板字段可配置-----------
    /** 提醒事项，默认 thing1 */
    @Value("${homeai.wechat.learn-remind-title-field:thing1}")
    private String learnRemindTitleField;

    /** 已学分钟，默认 number2（微信 number 类型） */
    @Value("${homeai.wechat.learn-remind-progress-field:number2}")
    private String learnRemindProgressField;

    /** 目标分钟，默认 number3；置空则跳过 */
    @Value("${homeai.wechat.learn-remind-goal-field:number3}")
    private String learnRemindGoalField;

    /** 提醒日期，默认 time4；置空则跳过 */
    @Value("${homeai.wechat.learn-remind-date-field:time4}")
    private String learnRemindDateField;

    /** thing1 文案 */
    @Value("${homeai.wechat.learn-remind-title-text:每日学习目标}")
    private String learnRemindTitleText;
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R31】学习提醒模板字段可配置-----------

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
            data.put("thing1", typedField("thing1", planTitle));
            data.put("time2", typedField("time2", planTimeText));
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

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29/R31】学习提醒-----------
    @Override
    public boolean sendLearnRemind(String openid, int goalMinutes, int todayMinutes) {
        if (oConvertUtils.isEmpty(openid)) {
            return false;
        }
        JSONObject data = buildLearnRemindData(goalMinutes, todayMinutes);
        String tip = "今日已学" + todayMinutes + "分钟，目标" + goalMinutes + "分钟";
        if (oConvertUtils.isEmpty(learnRemindTemplateId) || oConvertUtils.isEmpty(appid) || oConvertUtils.isEmpty(secret)) {
            log.info("[学习提醒-模拟推送] openid={}, tip={}, data={}", openid, tip, data);
            return true;
        }
        try {
            String accessToken = getAccessToken();
            JSONObject body = new JSONObject();
            body.put("touser", openid);
            body.put("template_id", learnRemindTemplateId);
            body.put("page", "pages-homeai-more/learn/index");
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
            log.warn("学习提醒推送失败: openid={}, resp={}", openid, response.body());
            return false;
        } catch (Exception e) {
            log.error("学习提醒推送异常: openid={}", openid, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> describeLearnRemindTemplate() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("titleField", nullToEmpty(learnRemindTitleField));
        fields.put("progressField", nullToEmpty(learnRemindProgressField));
        fields.put("goalField", nullToEmpty(learnRemindGoalField));
        fields.put("dateField", nullToEmpty(learnRemindDateField));
        fields.put("titleText", nullToEmpty(learnRemindTitleText));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("templateIdConfigured", oConvertUtils.isNotEmpty(learnRemindTemplateId));
        out.put("fields", fields);
        out.put("sampleData", buildLearnRemindData(30, 12));
        out.put("hint", "请在微信公众平台选用含 thing/number/time 的订阅消息模板，"
                + "并通过 homeai.wechat.learn-remind-*-field 与模板关键词对齐");
        return out;
    }

    private JSONObject buildLearnRemindData(int goalMinutes, int todayMinutes) {
        JSONObject data = new JSONObject();
        putLearnField(data, learnRemindTitleField, nullToEmpty(learnRemindTitleText));
        putLearnField(data, learnRemindProgressField, String.valueOf(Math.max(todayMinutes, 0)));
        putLearnField(data, learnRemindGoalField, String.valueOf(Math.max(goalMinutes, 0)));
        putLearnField(data, learnRemindDateField, LocalDate.now().format(CN_DATE));
        return data;
    }

    private void putLearnField(JSONObject data, String fieldKey, String value) {
        if (oConvertUtils.isEmpty(fieldKey) || data == null) {
            return;
        }
        String key = fieldKey.trim();
        data.put(key, typedField(key, value));
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29/R31】学习提醒-----------

    private JSONObject typedField(String fieldKey, String value) {
        JSONObject o = new JSONObject();
        o.put("value", truncateByFieldType(fieldKey, value));
        return o;
    }

    /** 按微信订阅消息字段类型截断 */
    private String truncateByFieldType(String fieldKey, String value) {
        String s = value == null ? "" : value.trim();
        String key = fieldKey == null ? "" : fieldKey.toLowerCase(Locale.ROOT);
        int max = 20;
        if (key.startsWith("thing")) {
            max = 20;
        } else if (key.startsWith("phrase")) {
            max = 5;
        } else if (key.startsWith("name")) {
            max = 10;
        } else if (key.startsWith("character_string")) {
            max = 32;
        } else if (key.startsWith("number") || key.startsWith("amount")) {
            max = 32;
        } else if (key.startsWith("time") || key.startsWith("date")) {
            max = 20;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
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
