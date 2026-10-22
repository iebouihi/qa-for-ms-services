package com.shop.cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Public API payloads of the cart service. */
public final class CartApi {

    private CartApi() {
    }

    public record CreateCartRequest(
            @NotNull UUID customerId,
            @NotEmpty @Size(max = 50) List<@Valid ItemRequest> items) {
    }

    public record ItemRequest(
            @NotNull UUID productId,
            @Min(1) @Max(CartPricing.MAX_QUANTITY_PER_PRODUCT) int quantity) {
    }

    public record CartResponse(UUID id, UUID customerId, String currency, BigDecimal total, Instant createdAt,
                               List<ItemResponse> items) {

        static CartResponse from(Cart cart) {
            return new CartResponse(cart.getId(), cart.getCustomerId(), cart.getCurrency(), cart.getTotal(),
                    cart.getCreatedAt(), cart.getItems().stream().map(ItemResponse::from).toList());
        }
    }

    public record ItemResponse(UUID productId, String productName, BigDecimal unitPrice, int quantity,
                               BigDecimal lineTotal) {

        static ItemResponse from(CartItem item) {
            return new ItemResponse(item.getProductId(), item.getProductName(), item.getUnitPrice(),
                    item.getQuantity(), item.getLineTotal());
        }
    }
}
