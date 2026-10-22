package com.shop.cart.customers;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
class KafkaConsumerConfig {

    /** 2 quick retries, then the record goes to {@code <topic>.DLT} with the failure in its headers. */
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafka) {
        // Destination named explicitly: library defaults have changed across versions (".DLT" vs "-dlt"),
        // and the dead-letter topic is part of the platform contract (contracts/asyncapi).
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafka,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(500L, 2L));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }

    /** Same partition count as the source topic: the recoverer publishes to the same partition. */
    @Bean
    NewTopic customerRegisteredDeadLetterTopic(@Value("${shop.topics.customer-registered}") String source,
                                               @Value("${shop.topics.replicas:1}") int replicas) {
        return TopicBuilder.name(source + ".DLT").partitions(3).replicas(replicas).build();
    }
}
