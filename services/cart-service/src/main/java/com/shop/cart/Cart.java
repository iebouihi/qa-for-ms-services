package com.shop.cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "cart")
public class Cart {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "cart_id", nullable = false)
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {
    }

    public Cart(UUID id, UUID customerId, String currency, BigDecimal total, Instant createdAt, List<CartItem> items) {
        this.id = id;
        this.customerId = customerId;
        this.currency = currency;
        this.total = total;
        this.createdAt = createdAt;
        this.items = new ArrayList<>(items);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<CartItem> getItems() {
        return List.copyOf(items);
    }
}
