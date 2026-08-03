package org.jeecg.modules.airag.filter;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * AIFlow 请求体兼容过滤器。
 *
 * 后端 AiragFlowServiceImpl 在解析 design JSON 时，
 * 对所有节点执行 options.get("outputText").booleanValue()，
 * 但未做空判断。当节点 options 中不包含 outputText 字段时，
 * Map.get() 返回 null，触发 NPE：
 *   Cannot invoke "java.lang.Boolean.booleanValue()" because the return
 *   value of "java.util.Map.get(Object)" is null
 *
 * 此 Filter 在请求到达 Controller 之前，自动为 design JSON 中
 * 所有节点的 options 补上缺失的 outputText / stream 默认值。
 *
 * @author AI Assistant
 * @date 2026-07-31 for：【AIFlow Boolean NPE】修复 add_flow / save_design 接口的 NPE
 */
@Slf4j
//update-begin---author:AI Assistant ---date:2026-07-31  for：【AIFlow Boolean NPE】修复 add_flow / save_design 接口的 NPE-----------
public class AiflowDesignSanitizeFilter extends OncePerRequestFilter {

    private static final Set<String> TARGET_PATH_SUFFIXES = new HashSet<>(Arrays.asList(
            "/airag/flow/add",
            "/airag/flow/design/save"
    ));

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)) {
            return true;
        }
        String uri = request.getRequestURI();
        for (String suffix : TARGET_PATH_SUFFIXES) {
            if (uri.contains(suffix)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        ModifiableBodyRequestWrapper wrapper = new ModifiableBodyRequestWrapper(request);
        String originalBody = wrapper.getBodyAsString();

        if (originalBody == null || originalBody.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String modifiedBody = sanitizeDesign(originalBody);
        if (!modifiedBody.equals(originalBody)) {
            wrapper.setBody(modifiedBody);
        }

        filterChain.doFilter(wrapper, response);
    }

    /**
     * 解析请求体中的 design 字段（JSON 字符串），确保每个节点的 options
     * 包含 outputText 和 stream 字段，防止后端 .booleanValue() NPE。
     */
    private String sanitizeDesign(String body) {
        try {
            JSONObject root = JSONObject.parse(body);
            if (root == null) {
                return body;
            }

            String designStr = root.getString("design");
            if (designStr == null || designStr.isEmpty()) {
                return body;
            }

            JSONObject designJson = JSONObject.parse(designStr);
            if (designJson == null) {
                return body;
            }

            JSONArray nodes = designJson.getJSONArray("nodes");
            if (nodes == null || nodes.isEmpty()) {
                return body;
            }

            boolean modified = false;
            for (int i = 0; i < nodes.size(); i++) {
                JSONObject node = nodes.getJSONObject(i);
                if (node == null) {
                    continue;
                }
                JSONObject properties = node.getJSONObject("properties");
                if (properties == null) {
                    properties = new JSONObject();
                    node.put("properties", properties);
                    modified = true;
                }

                JSONObject options = properties.getJSONObject("options");
                if (options == null) {
                    options = new JSONObject();
                    properties.put("options", options);
                    modified = true;
                }

                if (options.get("outputText") == null) {
                    // 根据 outputType 推断 outputText
                    // outputType="text" → 文本输出 → outputText=true
                    // 其他/不存在 → 非文本输出 → outputText=false
                    boolean isTextOutput = "text".equals(options.getString("outputType"));
                    options.put("outputText", isTextOutput);
                    modified = true;
                }

                if (options.get("stream") == null) {
                    options.put("stream", false);
                    modified = true;
                }
            }

            if (modified) {
                root.put("design", designJson.toJSONString());
                return root.toJSONString();
            }
            return body;
        } catch (Exception e) {
            log.warn("[AIFlow Filter] 解析请求体失败，跳过处理: {}", e.getMessage());
            return body;
        }
    }
}
//update-end---author:AI Assistant ---date:2026-07-31  for：【AIFlow Boolean NPE】修复 add_flow / save_design 接口的 NPE-----------
