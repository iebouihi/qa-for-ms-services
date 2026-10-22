package com.shop.cart.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.observability.KafkaCorrelation;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Same after-commit pattern as customer-service (an outbox would make it guaranteed). */
@Component
public class CartEventPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final String topic;

    public CartEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper json,
                              @Value("${shop.topics.cart-created}") String topic) {
        this.kafka = kafka;
        this.json = json;
        this.topic = topic;
    }

    @TransactionalEventListener
    public void on(CartCreatedEvent event) throws JsonProcessingException {
        kafka.send(KafkaCorrelation.propagate(
                new ProducerRecord<>(topic, event.cartId().toString(), json.writeValueAsString(event))));
    }

    @Bean
    static NewTopic cartCreatedTopic(@Value("${shop.topics.cart-created}") String name,
                                     @Value("${shop.topics.replicas:1}") int replicas) {
        return TopicBuilder.name(name).partitions(3).replicas(replicas).build();
    }
}
