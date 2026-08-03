package org.jeecg.modules.airag.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * AIFlow 过滤器配置。
 *
 * @author AI Assistant
 * @date 2026-07-31 for：【AIFlow Boolean NPE】注册请求体兼容过滤器
 */
@Configuration
//update-begin---author:AI Assistant ---date:2026-07-31  for：【AIFlow Boolean NPE】注册请求体兼容过滤器-----------
public class AiragFilterConfig {

    @Bean
    public AiflowDesignSanitizeFilter aiflowDesignSanitizeFilter() {
        return new AiflowDesignSanitizeFilter();
    }

    @Bean
    public FilterRegistrationBean<AiflowDesignSanitizeFilter> aiflowDesignSanitizeFilterRegistration() {
        FilterRegistrationBean<AiflowDesignSanitizeFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(aiflowDesignSanitizeFilter());
        registration.setName("aiflowDesignSanitizeFilter");
        registration.addUrlPatterns("/airag/flow/add", "/airag/flow/design/save");
        // 确保在 Shiro Filter 之前执行，以便修改后的请求体能被后续过滤器使用
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
//update-end---author:AI Assistant ---date:2026-07-31  for：【AIFlow Boolean NPE】注册请求体兼容过滤器-----------
