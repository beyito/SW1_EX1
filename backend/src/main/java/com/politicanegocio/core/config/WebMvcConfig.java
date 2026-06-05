package com.politicanegocio.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final DocumentAuditInterceptor documentAuditInterceptor;

    public WebMvcConfig(DocumentAuditInterceptor documentAuditInterceptor) {
        this.documentAuditInterceptor = documentAuditInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(documentAuditInterceptor)
                .addPathPatterns("/api/documents/**")
                .excludePathPatterns("/api/documents/process/**");
    }
}
