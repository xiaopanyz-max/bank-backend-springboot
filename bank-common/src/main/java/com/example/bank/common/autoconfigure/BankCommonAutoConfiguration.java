package com.example.bank.common.autoconfigure;

import com.example.bank.common.web.RequestTraceFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class BankCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RequestTraceFilter requestTraceFilter() {
        return new RequestTraceFilter();
    }

    @Bean
    public FilterRegistrationBean<RequestTraceFilter> requestTraceFilterRegistration(RequestTraceFilter filter) {
        FilterRegistrationBean<RequestTraceFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setName("requestTraceFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
