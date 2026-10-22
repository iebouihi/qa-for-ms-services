package com.shop.customer.events;

import java.time.Instant;
import java.util.UUID;

import com.shop.customer.Customer;

/**
 * Event published on {@code customer.registered}. Its shape is a contract with every
 * consumer (cart-service today): see contracts/asyncapi and the Pact message verification.
 */
public record CustomerRegisteredEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID customerId,
        String email,
        String firstName,
        String lastName) {

    public static final String TYPE = "CustomerRegistered";

    public static CustomerRegisteredEvent from(Customer customer, Instant occurredAt) {
        return new CustomerRegisteredEvent(UUID.randomUUID(), TYPE, occurredAt,
                customer.getId(), customer.getEmail(), customer.getFirstName(), customer.getLastName());
    }
}
