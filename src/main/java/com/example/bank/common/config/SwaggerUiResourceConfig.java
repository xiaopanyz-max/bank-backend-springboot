package com.example.bank.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the Swagger UI WebJar at a stable application URL.
 */
// 已关闭：springdoc-openapi-starter-webmvc-ui 会自动映射 /swagger-ui/index.html。
// 只有在 IDE 或运行环境未加载 UI Starter 时，才取消注释 @Configuration 启用该兜底映射。
// @Configuration
public class SwaggerUiResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations(
                        "classpath:/static/swagger-ui/",
                        "classpath:/META-INF/resources/webjars/swagger-ui/5.18.2/");
    }
}
