package com.shop.cart;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.shop.cart.CartApi.CreateCartRequest;
import com.shop.cart.CartPricing.CartRuleViolation;
import com.shop.cart.CartPricing.Line;
import com.shop.cart.CartPricing.PricedCart;
import com.shop.cart.customers.KnownCustomerRepository;
import com.shop.cart.events.CartCreatedEvent;
import com.shop.cart.products.ProductClient;
import com.shop.cart.products.ProductClient.ProductNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository carts;
    private final KnownCustomerRepository customers;
    private final ProductClient products;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public CartService(CartRepository carts, KnownCustomerRepository customers, ProductClient products,
                       ApplicationEventPublisher events) {
        this(carts, customers, products, events, Clock.systemUTC());
    }

    CartService(CartRepository carts, KnownCustomerRepository customers, ProductClient products,
                ApplicationEventPublisher events, Clock clock) {
        this.carts = carts;
        this.customers = customers;
        this.products = products;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public Cart create(CreateCartRequest request) {
        if (!customers.existsById(request.customerId())) {
            // Either the customer does not exist, or its event has not been processed yet
            // (eventual consistency). Callers may retry: the code tells them why.
            throw new CartRuleViolation("CUSTOMER_UNKNOWN", "Customer " + request.customerId() + " is not known yet");
        }
        Map<UUID, Integer> quantities = CartPricing.mergeQuantities(request.items());
        List<Line> lines = quantities.entrySet().stream()
                .map(e -> new Line(fetchProduct(e.getKey()), e.getValue()))
                .toList();
        PricedCart priced = CartPricing.price(lines);

        List<CartItem> items = priced.lines().stream()
                .map(l -> new CartItem(UUID.randomUUID(), l.product().id(), l.product().name(), l.product().price(),
                        l.quantity(), l.lineTotal()))
                .toList();
        Cart cart = carts.save(new Cart(UUID.randomUUID(), request.customerId(), priced.currency(), priced.total(),
                Instant.now(clock), items));
        events.publishEvent(CartCreatedEvent.from(cart));
        return cart;
    }

    @Transactional(readOnly = true)
    public Cart get(UUID id) {
        return carts.findById(id).orElseThrow(() -> new CartNotFoundException(id));
    }

    private com.shop.cart.products.ProductView fetchProduct(UUID id) {
        try {
            return products.get(id);
        } catch (ProductNotFoundException e) {
            throw new CartRuleViolation("PRODUCT_UNAVAILABLE", e.getMessage());
        }
    }

    public static class CartNotFoundException extends RuntimeException {
        public CartNotFoundException(UUID id) {
            super("Cart " + id + " not found");
        }
    }
}
