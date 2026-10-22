package com.shop.cart.products;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The part of a product that cart-service depends on: nothing more.
 * Unknown fields are ignored (tolerant reader), so product-service can add fields freely.
 * This exact shape is what the Pact contract with product-service records.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductView(UUID id, String name, BigDecimal price, String currency, boolean active) {
}
