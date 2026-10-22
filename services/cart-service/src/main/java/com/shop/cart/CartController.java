package com.shop.cart;

import java.net.URI;
import java.util.UUID;

import com.shop.cart.CartApi.CartResponse;
import com.shop.cart.CartApi.CreateCartRequest;
import com.shop.cart.CartPricing.CartRuleViolation;
import com.shop.cart.CartService.CartNotFoundException;
import com.shop.cart.products.ProductClient.ProductServiceUnavailableException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CartResponse> create(@Valid @RequestBody CreateCartRequest request) {
        Cart cart = service.create(request);
        return ResponseEntity.created(URI.create("/api/carts/" + cart.getId())).body(CartResponse.from(cart));
    }

    @GetMapping("/{id}")
    public CartResponse get(@PathVariable UUID id) {
        return CartResponse.from(service.get(id));
    }

    @ExceptionHandler(CartRuleViolation.class)
    ProblemDetail ruleViolation(CartRuleViolation e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problem.setProperty("code", e.code());
        return problem;
    }

    @ExceptionHandler(ProductServiceUnavailableException.class)
    ProblemDetail productServiceDown(ProductServiceUnavailableException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        problem.setProperty("code", "PRODUCT_SERVICE_UNAVAILABLE");
        return problem;
    }

    @ExceptionHandler(CartNotFoundException.class)
    ProblemDetail notFound(CartNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
