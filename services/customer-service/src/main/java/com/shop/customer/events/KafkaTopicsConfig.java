package com.shop.customer.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Topics are declared by their owner, never relied on auto-creation. */
@Configuration(proxyBeanMethods = false)
class KafkaTopicsConfig {

    @Bean
    NewTopic customerRegisteredTopic(@Value("${shop.topics.customer-registered}") String name,
                                     @Value("${shop.topics.replicas:1}") int replicas) {
        return TopicBuilder.name(name).partitions(3).replicas(replicas).build();
    }
}
