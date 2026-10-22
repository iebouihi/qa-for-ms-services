package com.shop.cart;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.shop.cart.products.ProductView;

/**
 * Pure pricing rules, no framework: the ideal target for fast, exhaustive unit tests.
 */
public final class CartPricing {

    public static final int MAX_QUANTITY_PER_PRODUCT = 99;

    private CartPricing() {
    }

    public record Line(ProductView product, int quantity) {
    }

    public record PricedLine(ProductView product, int quantity, BigDecimal lineTotal) {
    }

    public record PricedCart(List<PricedLine> lines, String currency, BigDecimal total) {
    }

    /** Requests may repeat a product: quantities are merged, then the merged quantity is checked. */
    public static Map<UUID, Integer> mergeQuantities(List<CartApi.ItemRequest> items) {
        Map<UUID, Integer> merged = new LinkedHashMap<>();
        items.forEach(item -> merged.merge(item.productId(), item.quantity(), Integer::sum));
        merged.forEach((productId, quantity) -> {
            if (quantity > MAX_QUANTITY_PER_PRODUCT) {
                throw new CartRuleViolation("QUANTITY_LIMIT_EXCEEDED",
                        "At most " + MAX_QUANTITY_PER_PRODUCT + " units of product " + productId);
            }
        });
        return merged;
    }

    public static PricedCart price(List<Line> lines) {
        if (lines.isEmpty()) {
            throw new CartRuleViolation("EMPTY_CART", "A cart needs at least one item");
        }
        String currency = lines.getFirst().product().currency();
        BigDecimal total = BigDecimal.ZERO;
        List<PricedLine> priced = new java.util.ArrayList<>();
        for (Line line : lines) {
            ProductView product = line.product();
            if (!product.active()) {
                throw new CartRuleViolation("PRODUCT_UNAVAILABLE", "Product " + product.id() + " is not for sale");
            }
            if (!product.currency().equals(currency)) {
                throw new CartRuleViolation("MIXED_CURRENCIES", "All products in a cart must share one currency");
            }
            BigDecimal lineTotal = product.price().multiply(BigDecimal.valueOf(line.quantity()))
                    .setScale(2, RoundingMode.HALF_EVEN);
            priced.add(new PricedLine(product, line.quantity(), lineTotal));
            total = total.add(lineTotal);
        }
        return new PricedCart(List.copyOf(priced), currency, total.setScale(2, RoundingMode.HALF_EVEN));
    }

    /** A business rule was broken: reported to the client as 422 with a stable code. */
    public static class CartRuleViolation extends RuntimeException {
        private final String code;

        public CartRuleViolation(String code, String message) {
            super(message);
            this.code = code;
        }

        public String code() {
            return code;
        }
    }
}
