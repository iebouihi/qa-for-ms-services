package com.shop.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.shop.cart.CartApi.ItemRequest;
import com.shop.cart.CartPricing.CartRuleViolation;
import com.shop.cart.CartPricing.Line;
import com.shop.cart.CartPricing.PricedCart;
import com.shop.cart.products.ProductView;
import org.junit.jupiter.api.Test;

/** Unit layer: the money rules, tested exhaustively and in milliseconds. */
class CartPricingTest {

    private static ProductView product(String price, String currency, boolean active) {
        return new ProductView(UUID.randomUUID(), "Product", new BigDecimal(price), currency, active);
    }

    @Test
    void totalIsSumOfLineTotals() {
        PricedCart cart = CartPricing.price(List.of(
                new Line(product("10.50", "EUR", true), 2),
                new Line(product("3.25", "EUR", true), 1)));

        assertThat(cart.total()).isEqualByComparingTo("24.25");
        assertThat(cart.currency()).isEqualTo("EUR");
        assertThat(cart.lines()).extracting(CartPricing.PricedLine::lineTotal)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("21.00"), new BigDecimal("3.25"));
    }

    @Test
    void decimalArithmeticNeverLosesCents() {
        // 0.1 + 0.2 is the classic floating point trap; BigDecimal keeps it exact.
        PricedCart cart = CartPricing.price(List.of(
                new Line(product("0.10", "EUR", true), 1),
                new Line(product("0.20", "EUR", true), 1)));

        assertThat(cart.total()).isEqualByComparingTo("0.30");
    }

    @Test
    void inactiveProductCannotBeAdded() {
        assertThatThrownBy(() -> CartPricing.price(List.of(new Line(product("5.00", "EUR", false), 1))))
                .isInstanceOf(CartRuleViolation.class)
                .extracting(e -> ((CartRuleViolation) e).code()).isEqualTo("PRODUCT_UNAVAILABLE");
    }

    @Test
    void mixedCurrenciesAreRejected() {
        assertThatThrownBy(() -> CartPricing.price(List.of(
                new Line(product("5.00", "EUR", true), 1),
                new Line(product("5.00", "USD", true), 1))))
                .extracting(e -> ((CartRuleViolation) e).code()).isEqualTo("MIXED_CURRENCIES");
    }

    @Test
    void repeatedProductsAreMergedBeforeCheckingTheLimit() {
        UUID id = UUID.randomUUID();

        Map<UUID, Integer> merged = CartPricing.mergeQuantities(List.of(new ItemRequest(id, 40), new ItemRequest(id, 59)));
        assertThat(merged).containsExactly(Map.entry(id, 99));

        assertThatThrownBy(() -> CartPricing.mergeQuantities(List.of(new ItemRequest(id, 50), new ItemRequest(id, 50))))
                .extracting(e -> ((CartRuleViolation) e).code()).isEqualTo("QUANTITY_LIMIT_EXCEEDED");
    }
}
