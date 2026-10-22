package com.shop.cart.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.shop.cart.Cart;

/** Published on {@code cart.created} once a cart is committed (documented in contracts/asyncapi). */
public record CartCreatedEvent(UUID eventId, String eventType, Instant occurredAt, UUID cartId, UUID customerId,
                               String currency, BigDecimal total, List<Line> items) {

    public static final String TYPE = "CartCreated";

    public record Line(UUID productId, int quantity, BigDecimal unitPrice) {
    }

    public static CartCreatedEvent from(Cart cart) {
        return new CartCreatedEvent(UUID.randomUUID(), TYPE, cart.getCreatedAt(), cart.getId(), cart.getCustomerId(),
                cart.getCurrency(), cart.getTotal(),
                cart.getItems().stream().map(i -> new Line(i.getProductId(), i.getQuantity(), i.getUnitPrice())).toList());
    }
}
