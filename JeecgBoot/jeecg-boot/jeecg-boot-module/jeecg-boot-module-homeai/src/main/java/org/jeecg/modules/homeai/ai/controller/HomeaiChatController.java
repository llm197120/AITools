package org.jeecg.modules.homeai.ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.homeai.config.HomeaiFileMagicUtil;
import org.jeecg.modules.homeai.config.HomeaiJwtUtil;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.config.service.IHomeaiFileWhitelistService;
import org.jeecg.modules.homeai.ai.service.IHomeaiChatService;
import org.jeecg.modules.homeai.ai.service.IAiQuotaService;
import org.jeecg.modules.homeai.ai.service.IHomeaiAiQuotaPrecheckService;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.service.IWxUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

/**
 * 家庭AI对话接口（SSE流式）
 */
@Slf4j
@RestController
@RequestMapping("/homeai/ai/chat")
public class HomeaiChatController {

    @Autowired
    private IHomeaiChatService chatService;

    @Autowired
    private IAiQuotaService quotaService;

    @Autowired
    private IHomeaiAiQuotaPrecheckService precheckService;

    @Autowired
    private IWxUserService wxUserService;

    @Autowired
    private IHomeaiFileWhitelistService whitelistService;

    @Autowired
    private IHomeaiFileStorageService fileStorageService;

    /**
     * 从 Token 解析用户ID
     */
    private String getUserId(HttpServletRequest request) {
        String token = request.getHeader("X-Access-Token");
        String openid = HomeaiJwtUtil.getOpenid(token);
        if (openid == null) return null;
        WxUser user = wxUserService.getByOpenid(openid);
        return user != null ? user.getId() : null;
    }

    /**
     * 发送消息（SSE流式响应）
     * 小程序端使用 uni.request enableChunked 模式接收
     */
    @PostMapping("/send")
    public SseEmitter send(@RequestParam(required = false) String conversationId,
                           @RequestParam String content,
                           @RequestParam(required = false) List<String> images,
                           @RequestParam(required = false) List<String> files,
                           HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("error").data("未登录"));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE 发送错误", e);
            }
            return emitter;
        }
        images = sanitizeAttachmentUrls(images);
        files = sanitizeAttachmentUrls(files);
        return chatService.sendMessage(userId, conversationId, content, images, files);
    }

    /** 过滤前端误传的 undefined/null 等无效附件地址 */
    private List<String> sanitizeAttachmentUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> cleaned = new ArrayList<>();
        for (String url : urls) {
            if (url == null) {
                continue;
            }
            String trimmed = url.trim();
            if (trimmed.isEmpty() || "undefined".equalsIgnoreCase(trimmed) || "null".equalsIgnoreCase(trimmed)) {
                continue;
            }
            cleaned.add(fileStorageService.normalizeStoredReference(trimmed));
        }
        return cleaned;
    }

    /**
     * 检查Token配额（兼容旧前端；推荐使用 /homeai/ai/quota/precheck）
     */
    @GetMapping("/quota")
    public Result<?> checkQuota(@RequestParam(required = false) String content,
                                HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R25】chat 配额走统一预检-----------
        return Result.OK(precheckService.precheck(userId, "chat", content));
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R25】chat 配额走统一预检-----------
    }

    /**
     * 停止生成
     */
    @PostMapping("/stop")
    public Result<?> stop(@RequestParam String conversationId) {
        chatService.stopGeneration(conversationId);
        return Result.OK("已停止");
    }

    //update-begin---author:admin ---date:2026-07-31  for：AI对话文件附件上传-----------
    /**
     * 上传对话附件文件
     */
    @PostMapping("/upload")
    public Result<?> uploadFile(@RequestParam MultipartFile file, HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        try {
            String ext = getExtension(file.getOriginalFilename());
            if (ext != null && !whitelistService.isAllowedExtension(ext)) {
                return Result.error("不支持上传该文件类型");
            }
            if (ext != null) {
                try {
                    HomeaiFileMagicUtil.validate(file, ext);
                } catch (IOException e) {
                    return Result.error(e.getMessage());
                }
            }
            String fileName = UUID.randomUUID().toString().replace("-", "") + (ext != null ? "." + ext : "");
            String objectKey = "homeai/chat/" + userId + "/" + fileName;
            String storedUrl = fileStorageService.storeMultipart(file, objectKey);
            Map<String, Object> result = new HashMap<>();
            result.put("storedUrl", storedUrl);
            result.put("url", fileStorageService.resolveAccessUrl(storedUrl));
            result.put("name", file.getOriginalFilename());
            result.put("size", file.getSize());
            result.put("type", file.getContentType());
            return Result.OK(result);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return null;
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx + 1) : null;
    }
    //update-end---author:admin ---date:2026-07-31  for：AI对话文件附件上传-----------
}
