package org.jeecg.modules.homeai.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.homeai.ai.entity.AiConversation;
import org.jeecg.modules.homeai.ai.entity.AiMessage;
import org.jeecg.modules.homeai.ai.service.IAiConversationService;
import org.jeecg.modules.homeai.ai.service.IAiMessageService;
import org.jeecg.modules.homeai.config.HomeaiFileUrlUtil;
import org.jeecg.modules.homeai.config.HomeaiSecurityUtil;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI对话管理
 */
@RestController
@RequestMapping("/homeai/ai/conversations")
public class AiConversationController {

    @Autowired
    private IAiConversationService conversationService;

    @Autowired
    private IAiMessageService messageService;

    @Autowired
    private HomeaiSecurityUtil securityUtil;

    /**
     * 小程序端用户 ID（仅 HomeAI JWT）
     */
    private String getAppUserId(HttpServletRequest request) {
        WxUser user = securityUtil.getWxUser(request);
        return user != null ? user.getId() : null;
    }

    /**
     * 对话列表（管理端分页）
     */
    @GetMapping("/list")
    public Result<?> list(AiConversation conversation,
                          @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                          HttpServletRequest req) {
        QueryWrapper<AiConversation> queryWrapper = QueryGenerator.initQueryWrapper(conversation, req.getParameterMap());
        Page<AiConversation> page = new Page<>(pageNo, pageSize);
        IPage<AiConversation> pageList = conversationService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 获取当前用户对话列表（小程序端）
     */
    @GetMapping("/mine")
    public Result<?> getMyConversations(HttpServletRequest request) {
        String userId = getAppUserId(request);
        if (userId == null) return Result.error("未登录");
        List<AiConversation> list = conversationService.getUserConversations(userId);
        return Result.OK(list);
    }

    /**
     * 创建新对话
     */
    @PostMapping
    public Result<?> create(@RequestBody AiConversation conversation, HttpServletRequest request) {
        String userId = getAppUserId(request);
        if (userId == null) return Result.error("未登录");
        AiConversation result = conversationService.createConversation(
                userId, conversation.getTitle(), conversation.getModelName());
        return Result.OK(result);
    }

    /**
     * 重命名对话
     */
    @PutMapping("/{id}/rename")
    public Result<?> rename(@PathVariable String id, @RequestParam String title, HttpServletRequest request) {
        if (!canManageConversation(id, request)) {
            return Result.error("无权操作该对话");
        }
        conversationService.renameConversation(id, title);
        return Result.OK("重命名成功");
    }

    /**
     * 软删除对话
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id, HttpServletRequest request) {
        if (!canManageConversation(id, request)) {
            return Result.error("无权操作该对话");
        }
        conversationService.softDelete(id);
        return Result.OK("删除成功");
    }

    /**
     * 获取对话消息列表
     */
    @GetMapping("/{id}/messages")
    public Result<?> getMessages(@PathVariable String id, HttpServletRequest request) {
        if (!canManageConversation(id, request)) {
            return Result.error("无权查看该对话");
        }
        List<AiMessage> messages = messageService.getConversationMessages(id);
        // 兼容历史相对地址数据：统一转换为绝对访问地址
        if (messages != null) {
            for (AiMessage m : messages) {
                if (m.getFileUrl() != null && !m.getFileUrl().startsWith("http")) {
                    m.setFileUrl(HomeaiFileUrlUtil.toAbsoluteUrl(m.getFileUrl()));
                }
            }
        }
        return Result.OK(messages);
    }

    /**
     * 管理端控制台可操作任意对话；小程序端仅能操作自己的对话
     */
    private boolean canManageConversation(String conversationId, HttpServletRequest request) {
        if (securityUtil.isConsoleAuthenticated(request)) {
            return true;
        }
        String userId = getAppUserId(request);
        if (userId == null) {
            return false;
        }
        AiConversation conversation = conversationService.getById(conversationId);
        return conversation != null && userId.equals(conversation.getUserId());
    }
}
