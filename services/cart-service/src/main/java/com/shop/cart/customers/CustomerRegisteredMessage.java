package com.shop.cart.customers;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Consumer-owned view of the CustomerRegistered event: only the fields cart-service needs.
 * Recorded as a Pact message contract and verified by customer-service on every build.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerRegisteredMessage(UUID eventId, String eventType, Instant occurredAt, UUID customerId,
                                        String email) {
}
