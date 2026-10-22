package com.shop.cart.customers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Builds the known-customer projection.
 *
 * <ul>
 *   <li><b>Idempotent:</b> Kafka delivers at least once, so the same event may arrive twice.
 *       Upserting by customer id makes a replay harmless.</li>
 *   <li><b>Poison messages:</b> an unreadable payload throws, is retried, then parked on
 *       {@code customer.registered.DLT} (see KafkaConsumerConfig) instead of blocking the partition.</li>
 * </ul>
 */
@Component
public class CustomerRegisteredHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomerRegisteredHandler.class);

    private final KnownCustomerRepository repository;
    private final ObjectMapper json;

    public CustomerRegisteredHandler(KnownCustomerRepository repository, ObjectMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @KafkaListener(topics = "${shop.topics.customer-registered}")
    public void onMessage(String payload) {
        handle(payload);
    }

    public void handle(String payload) {
        CustomerRegisteredMessage event = parse(payload);
        if (event.customerId() == null || event.email() == null || event.occurredAt() == null) {
            throw new IllegalArgumentException("Incomplete CustomerRegistered event: " + payload);
        }
        // save() on an existing id is an update: replaying the event changes nothing.
        repository.save(new KnownCustomer(event.customerId(), event.email(), event.occurredAt()));
        log.info("Customer {} is now known to cart-service", event.customerId());
    }

    private CustomerRegisteredMessage parse(String payload) {
        try {
            return json.readValue(payload, CustomerRegisteredMessage.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unreadable CustomerRegistered event", e);
        }
    }
}
