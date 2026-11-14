package com.umdc.mercury.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers {@link MachineTokenInterceptor} on the endpoints only backend callers may use, and {@link TemplateCallerInterceptor} on the templates. */
@Configuration
public class MachineTokenWebConfigurer implements WebMvcConfigurer {

    private final MachineTokenInterceptor interceptor;
    private final TemplateCallerInterceptor templateInterceptor;

    public MachineTokenWebConfigurer(MachineTokenInterceptor interceptor, TemplateCallerInterceptor templateInterceptor) {
        this.interceptor = interceptor;
        this.templateInterceptor = templateInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/v1/mail/**", "/api/v1/messages/**", "/api/v1/verification-code/**", "/api/v1/telegram/links/**", "/api/v1/telegram/messages/**");
        registry.addInterceptor(templateInterceptor).addPathPatterns("/api/v1/templates/**");
    }
}
