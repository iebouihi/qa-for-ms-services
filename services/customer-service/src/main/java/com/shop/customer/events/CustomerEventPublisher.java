package com.shop.customer.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.observability.KafkaCorrelation;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the event only once the customer row is committed, so consumers never see a
 * customer that does not exist.
 *
 * <p>Known limitation, kept on purpose for the demo: if Kafka is down right after the
 * commit, the event is lost. The production answer is the transactional outbox pattern
 * (event row written in the same transaction, relayed by Debezium or a poller).
 */
@Component
public class CustomerEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CustomerEventPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final String topic;

    public CustomerEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper json,
                                  @Value("${shop.topics.customer-registered}") String topic) {
        this.kafka = kafka;
        this.json = json;
        this.topic = topic;
    }

    @TransactionalEventListener
    public void on(CustomerRegisteredEvent event) {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(topic, event.customerId().toString(), serialize(event));
        kafka.send(KafkaCorrelation.propagate(record));
        log.info("Published {} for customer {}", event.eventType(), event.customerId());
    }

    String serialize(CustomerRegisteredEvent event) {
        try {
            return json.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize " + event, e);
        }
    }
}
