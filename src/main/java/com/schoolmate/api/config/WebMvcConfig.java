package com.schoolmate.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CacheControlInterceptor cacheControlInterceptor;
    private final AnoEscolarHeaderInterceptor anoEscolarHeaderInterceptor;
    private final AnoEscolarArgumentResolver anoEscolarArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cacheControlInterceptor);

        registry.addInterceptor(anoEscolarHeaderInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/anos-escolares",
                        "/api/anos-escolares/**",
                        "/api/grados",
                        "/api/grados/**",
                        "/api/dev/**",
                        "/error"
                );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(anoEscolarArgumentResolver);
    }
}
