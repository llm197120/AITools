package org.jeecg.modules.homeai.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * HomeAI 鉴权拦截器注册（在 Shiro 过滤器链之后、Controller 之前执行）
 */
@Configuration
public class HomeaiMvcConfig implements WebMvcConfigurer {

    @Autowired
    private HomeaiAuthInterceptor homeaiAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(homeaiAuthInterceptor).addPathPatterns("/homeai/**");
    }
}
