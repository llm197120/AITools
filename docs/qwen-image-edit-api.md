# 阿里云百炼 Qwen-Image-Edit API 接入指南（JeecgBoot / Java）

> 更新时间：2026-08-17 ｜ 数据来源：阿里云百炼官方帮助中心（help.aliyun.com/zh/model-studio/qwen-image-edit-api）
> 用途：在家庭AI小工具后端（JeecgBoot Java 17 / Spring Boot）中接入「指令式图片编辑」能力。
> 定价：**¥0.3/张**（qwen-image-edit，千问AI平台口径），详见百炼控制台。

---

## 一、接口总览

| 项目 | 值 |
|---|---|
| 协议 | HTTP POST，同步调用（不支持异步） |
| 北京地域 Endpoint | `https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation` |
| 北京地域（业务空间专属，推荐） | `https://{WorkspaceId}.cn-beijing.maas.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation` |
| 新加坡地域 | `https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation` |
| 鉴权 | Header `Authorization: Bearer sk-xxxx`（**API Key 按地域独立，不可混用**） |
| 模型名 | `qwen-image-edit`（Plus）、`qwen-image-2.0-pro`、`qwen-image-3.0-pro` / `qwen-image-3.0` |
| 输入 | 1~3 张图（URL 或 Base64）+ 1 条文本指令；**仅单轮对话**，messages 数组有且只有一个对象 |
| 输出 | 生成图 URL（PNG，**有效期 24 小时**，需及时下载转存） |
| 图像格式 | JPG / JPEG / PNG / BMP / TIFF / WEBP / GIF |

## 二、请求体结构（官方示例）

```json
{
  "model": "qwen-image-2.0-pro",
  "input": {
    "messages": [
      {
        "role": "user",
        "content": [
          { "image": "https://example.com/input1.png" },
          { "image": "https://example.com/input2.png" },
          { "text": "图1中的女生穿着图2中的黑色裙子按图3的姿势坐下" }
        ]
      }
    ]
  },
  "parameters": {
    "n": 2,
    "negative_prompt": "低分辨率、错误、最差质量、低质量",
    "prompt_extend": true,
    "watermark": false,
    "size": "1024*1536"
  }
}
```

**参数说明（parameters）**：

| 参数 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `n` | int | 1 | 生成张数（1~4） |
| `negative_prompt` | string | - | 反向提示词 |
| `prompt_extend` | bool | true | 提示词扩写 |
| `watermark` | bool | false | 是否加水印 |
| `size` | string | - | 输出尺寸，如 `1024*1536`、`1536*1024`、`2048*2048` |
| `seed` | int | 随机 | 相同 seed 出图较一致 |

**图片传参两种方式**：
- URL：`{"image": "https://..."}`
- Base64：`{"image": "data:image/png;base64,...."}`

## 三、响应体结构

```json
{
  "output": {
    "choices": [
      {
        "finish_reason": "stop",
        "message": {
          "role": "assistant",
          "content": [
            { "image": "https://dashscope-result-hz.oss-cn-hangzhou.aliyuncs.com/xxx.png?Expires=xxx" }
          ]
        }
      }
    ]
  },
  "usage": { "image_count": 1, "width": 2048, "height": 2048 },
  "request_id": "902fee3b-..."
}
```

- 成功：`output.choices[0].message.content[].image` 为生成图临时 URL（24h 内有效）。
- 失败：返回 `code` + `message`，参见[百炼错误码](https://help.aliyun.com/zh/model-studio/error-code)。

## 四、JeecgBoot / Java 接入示例

### 1. 依赖（Spring Web + Jackson，JeecgBoot 已内置）

```xml
<!-- 无需新增依赖；若用 DashScope 官方 SDK 则追加： -->
<dependency>
    <groupId>com.alibaba.dashscope</groupId>
    <artifactId>dashscope-sdk-java</artifactId>
    <version>最新版</version>
</dependency>
```

### 2. 配置（application.yml，注意勿提交到 git）

```yaml
homeai:
  bailian:
    api-key: ${BAILIAN_API_KEY:}          # 从环境变量注入
    base-url: https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation
    model: qwen-image-2.0-pro
```

### 3. 调用服务（Spring `RestTemplate`，无需额外 SDK）

```java
//update-begin---author:xxx---date:2026-08-17---for:【需求】接入百炼 Qwen-Image-Edit 图片指令编辑---
package org.jeecg.modules.homeai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BailianImageEditService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${homeai.bailian.api-key}")
    private String apiKey;

    @Value("${homeai.bailian.base-url}")
    private String baseUrl;

    @Value("${homeai.bailian.model}")
    private String model;

    /**
     * 指令式图片编辑
     *
     * @param imageUrls 输入图片 URL（1~3 张），或 Base64（data:image/xxx;base64,....）
     * @param prompt    编辑指令（中文优先）
     * @param size      输出尺寸，如 "1024*1536"，null 则用模型默认
     * @param n         生成张数（1~4）
     * @return 生成图片的临时 URL 列表
     */
    public List<String> editImage(List<String> imageUrls, String prompt, String size, Integer n) {
        // 1. 组装 content：先图后文（单轮，role=user）
        ArrayNode content = objectMapper.createArrayNode();
        for (String url : imageUrls) {
            content.add(objectMapper.createObjectNode().put("image", url));
        }
        content.add(objectMapper.createObjectNode().put("text", prompt));

        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "user");
        message.set("content", content);

        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(message);

        ObjectNode input = objectMapper.createObjectNode();
        input.set("messages", messages);

        // 2. parameters
        ObjectNode params = objectMapper.createObjectNode();
        params.put("n", n == null ? 1 : n);
        params.put("negative_prompt", "低分辨率、错误、最差质量、低质量");
        params.put("prompt_extend", true);
        params.put("watermark", false);
        if (size != null) {
            params.put("size", size);
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.set("input", input);
        body.set("parameters", params);

        // 3. HTTP 调用
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    baseUrl, HttpMethod.POST, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.error("百炼图片编辑 HTTP 失败: status={}, body={}", resp.getStatusCode(), resp.getBody());
                throw new RuntimeException("图片编辑服务调用失败: HTTP " + resp.getStatusCode());
            }
            JsonNode root = objectMapper.readTree(resp.getBody());

            // 4. 解析 output.choices[0].message.content[].image
            List<String> resultUrls = new ArrayList<>();
            JsonNode choices = root.path("output").path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode contentNode = choices.get(0).path("message").path("content");
                for (JsonNode item : contentNode) {
                    String imageUrl = item.path("image").asText("");
                    if (!imageUrl.isEmpty()) {
                        resultUrls.add(imageUrl);
                    }
                }
            }
            if (resultUrls.isEmpty()) {
                log.error("百炼图片编辑返回空结果: {}", resp.getBody());
                throw new RuntimeException("图片编辑未生成结果: " + root.path("message").asText());
            }
            return resultUrls;
        } catch (Exception e) {
            log.error("百炼图片编辑调用异常", e);
            throw new RuntimeException("图片编辑服务调用失败", e);
        }
    }
}
//update-end---author:xxx---date:2026-08-17---for:【需求】接入百炼 Qwen-Image-Edit 图片指令编辑---
```

### 4. 图片下载转存（URL 24h 有效，务必及时转存到本地/OSS）

```java
// 将生成 URL 下载为 byte[]，落盘或上传自有 OSS（示例：本地落盘）
public String downloadToLocal(String url, String saveDir) throws IOException {
    byte[] bytes = restTemplate.getForObject(url, byte[].class);
    String fileName = "edit_" + System.currentTimeMillis() + ".png";
    Path target = Paths.get(saveDir, fileName);
    Files.write(target, bytes);
    return target.toString();
}
```

### 5. 前端调用（管理端 `defHttp`，注意三参数约定）

```typescript
// 小程序/管理端上传原图 → 拿 URL → 调后端编辑接口
defHttp.uploadFile(
  { url: '/homeai/image/edit', filePath: tempFilePath, name: 'file', formData: { prompt: '把划痕去掉' } },
  () => {},
  { isReturnResponse: true }  // 关键：必须传第3参才能读 res.result
).then((res: any) => {
  const data = res.result; // { code, result: ['https://...生成图...'] }
});
```

## 五、注意事项

1. **API Key 安全**：放服务端配置（环境变量），禁止前端/小程序下发；异地请求用业务空间专属域名更稳。
2. **地域隔离**：北京与新加坡 API Key / 地址独立，跨地域调用直接鉴权失败。
3. **图片上传**：本地图需先转 Base64 或先传自有存储再给 URL；Base64 有体积上限（具体见官方文档）。
4. **生成图转存**：返回 URL 24h 过期，落库前必须下载到自有 OSS / 服务器。
5. **失败重试**：对 `Throttling` / 5xx 做指数退避重试（建议最多 3 次）。
6. **敏感内容**：官方有内容安全过滤，命中会返回错误码，前端需给出友好提示。

## 六、官方参考

- 接口文档：https://help.aliyun.com/zh/model-studio/qwen-image-edit-api
- 千问AI平台：https://platform.qianwenai.com/models/qwen-image-edit
- 错误码：https://help.aliyun.com/zh/model-studio/error-code
- DashScope SDK（Python/Java）：https://help.aliyun.com/zh/model-studio/install-sdk
