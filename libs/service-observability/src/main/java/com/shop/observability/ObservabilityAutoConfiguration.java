package com.shop.observability;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.kafka.listener.RecordInterceptor;

/**
 * Drop-in for every service: adding the dependency is enough. This is the "golden path"
 * idea applied to testability: squads get traceable journeys without writing any code.
 */
@AutoConfiguration
public class ObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /** Outgoing REST calls carry the current correlation id. */
    @Bean
    RestClientCustomizer correlationIdRestClientCustomizer() {
        return builder -> builder.requestInterceptor((request, body, execution) -> {
            request.getHeaders().set(CorrelationId.HEADER, CorrelationId.currentOrNew());
            return execution.execute(request, body);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RecordInterceptor.class)
    static class KafkaCorrelationConfiguration {

        @Bean
        RecordInterceptor<Object, Object> correlationIdRecordInterceptor() {
            return KafkaCorrelation.recordInterceptor();
        }
    }
}
