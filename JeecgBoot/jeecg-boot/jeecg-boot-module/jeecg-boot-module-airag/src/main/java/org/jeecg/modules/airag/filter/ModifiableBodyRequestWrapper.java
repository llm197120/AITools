package org.jeecg.modules.airag.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 可修改请求体的 HttpServletRequestWrapper。
 *
 * 构造时缓存原始请求体，后续可通过 {@link #setBody(String)} 替换请求体内容。
 * 用于在 Filter 中修改 POST/PUT 请求的 JSON body 后再传递给下游。
 *
 * @author AI Assistant
 * @date 2026-07-31 for：【AIFlow Boolean NPE】修复 add_flow / save_design 接口的请求体
 */
//update-begin---author:AI Assistant ---date:2026-07-31  for：【AIFlow Boolean NPE】可修改请求体的 HttpServletRequestWrapper-----------
public class ModifiableBodyRequestWrapper extends HttpServletRequestWrapper {

    private byte[] body;

    public ModifiableBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.body = request.getInputStream().readAllBytes();
    }

    public String getBodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    public void setBody(String newBody) {
        this.body = newBody.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() {
                return bais.read();
            }

            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                // 不需要异步读取
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
//update-end---author:AI Assistant ---date:2026-07-31  for：【AIFlow Boolean NPE】可修改请求体的 HttpServletRequestWrapper-----------
