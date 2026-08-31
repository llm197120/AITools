package org.jeecg.modules.homeai.config;

import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;

import java.io.IOException;

/**
 * 流式下载等非 JSON 接口的拒绝响应（401/403/404）。
 */
public final class HomeaiHttpDeny {

    private HomeaiHttpDeny() {
    }

    public static void write(HttpServletResponse response, Result<?> denied) throws IOException {
        String msg = denied == null || denied.getMessage() == null ? "无权访问" : denied.getMessage();
        int status = HttpServletResponse.SC_FORBIDDEN;
        if (msg.contains("未登录")) {
            status = HttpServletResponse.SC_UNAUTHORIZED;
        } else if (msg.contains("不存在")) {
            status = HttpServletResponse.SC_NOT_FOUND;
        }
        response.sendError(status, msg);
    }
}
