package com.example.auditlog.adapter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdapterConfiguration {
    @Bean
    @ConditionalOnMissingBean
    AuditEventPublisher auditEventPublisher() {
        return new NoOpAuditEventPublisher();
    }
}
