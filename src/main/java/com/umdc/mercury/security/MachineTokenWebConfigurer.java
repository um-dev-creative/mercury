package com.umdc.mercury.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers {@link MachineTokenInterceptor} on the endpoints only backend callers may use. */
@Configuration
public class MachineTokenWebConfigurer implements WebMvcConfigurer {

    private final MachineTokenInterceptor interceptor;

    public MachineTokenWebConfigurer(MachineTokenInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/v1/mail/**", "/api/v1/verification-code/**", "/api/v1/telegram/links/**");
    }
}
