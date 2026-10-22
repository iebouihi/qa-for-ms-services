package com.shop.cart.customers;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cart's own read model of customers, built from events. Cart never calls customer-service. */
@Entity
@Table(name = "known_customer")
public class KnownCustomer {

    @Id
    @Column(name = "customer_id")
    private UUID customerId;

    @Column(nullable = false)
    private String email;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    protected KnownCustomer() {
    }

    public KnownCustomer(UUID customerId, String email, Instant registeredAt) {
        this.customerId = customerId;
        this.email = email;
        this.registeredAt = registeredAt;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getEmail() {
        return email;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }
}
