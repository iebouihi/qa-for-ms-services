package com.shop.cart.products;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Synchronous call to product-service to snapshot name and price when a cart is created. */
@Component
public class ProductClient {

    private final RestClient http;

    public ProductClient(RestClient.Builder builder,
                         @Value("${shop.clients.product.base-url}") String baseUrl,
                         @Value("${shop.clients.product.connect-timeout:2s}") Duration connectTimeout,
                         @Value("${shop.clients.product.read-timeout:3s}") Duration readTimeout) {
        // Explicit timeouts: a slow dependency must fail fast, never hang the cart API.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.http = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public ProductView get(UUID productId) {
        try {
            return http.get().uri("/api/products/{id}", productId).retrieve().body(ProductView.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new ProductNotFoundException(productId);
            }
            throw new ProductServiceUnavailableException(e);
        } catch (RestClientException e) {
            throw new ProductServiceUnavailableException(e);
        }
    }

    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(UUID id) {
            super("Product " + id + " does not exist");
        }
    }

    public static class ProductServiceUnavailableException extends RuntimeException {
        public ProductServiceUnavailableException(Throwable cause) {
            super("product-service is unavailable", cause);
        }
    }
}
