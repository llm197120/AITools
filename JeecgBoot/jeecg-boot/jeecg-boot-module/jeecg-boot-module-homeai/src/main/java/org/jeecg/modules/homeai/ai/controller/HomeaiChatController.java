package org.jeecg.modules.homeai.ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.homeai.config.HomeaiFileUrlUtil;
import org.jeecg.modules.homeai.config.HomeaiJwtUtil;
import org.jeecg.modules.homeai.ai.service.IHomeaiChatService;
import org.jeecg.modules.homeai.ai.service.IAiQuotaService;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.service.IWxUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.*;
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
    private IWxUserService wxUserService;

    @Value("${jeecg.path.upload:./upload}")
    private String uploadPath;

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
        return chatService.sendMessage(userId, conversationId, content, images, files);
    }

    /**
     * 检查Token配额
     */
    @GetMapping("/quota")
    public Result<?> checkQuota(HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");

        Map<String, Object> quota = quotaService.checkQuota(userId, 0, 0);
        Map<String, Integer> defaultQuota = quotaService.getDefaultQuota();
        quota.put("dailyLimit", defaultQuota.get("dailyLimit"));
        quota.put("monthlyLimit", defaultQuota.get("monthlyLimit"));
        return Result.OK(quota);
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
            String dir = uploadPath + "/homeai/chat/" + userId + "/";
            Files.createDirectories(Path.of(dir));
            String ext = getExtension(file.getOriginalFilename());
            if (ext != null && !HomeaiFileUrlUtil.isAllowedUploadExtension(ext)) {
                return Result.error("不支持上传该文件类型");
            }
            String fileName = UUID.randomUUID().toString().replace("-", "") + (ext != null ? "." + ext : "");
            Path targetPath = Path.of(dir + fileName);
            file.transferTo(targetPath.toFile());
            Map<String, Object> result = new HashMap<>();
            result.put("url", HomeaiFileUrlUtil.toAbsoluteUrl("/upload/homeai/chat/" + userId + "/" + fileName));
            result.put("name", file.getOriginalFilename());
            result.put("size", file.getSize());
            result.put("type", file.getContentType());
            return Result.OK(result);
        } catch (IOException e) {
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
