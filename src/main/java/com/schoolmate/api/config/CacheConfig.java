package com.schoolmate.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.io.InputStream;

@Configuration
public class CacheConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiShallowEtagHeaderFilter());
        registration.addUrlPatterns("/api/*");
        return registration;
    }

    private static final class ApiShallowEtagHeaderFilter extends ShallowEtagHeaderFilter {
        @Override
        protected boolean isEligibleForEtag(
                HttpServletRequest request,
                HttpServletResponse response,
                int responseStatusCode,
                InputStream inputStream
        ) {
            return !response.isCommitted()
                    && responseStatusCode >= 200
                    && responseStatusCode < 300
                    && "GET".equalsIgnoreCase(request.getMethod());
        }
    }
}
