package com.shop.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.shop.cart.products.ProductClient;
import com.shop.cart.products.ProductClient.ProductNotFoundException;
import com.shop.cart.products.ProductView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;

/**
 * Contract layer (consumer side, HTTP): cart-service records exactly what it needs from
 * product-service, using its real ProductClient against a Pact mock server.
 * The resulting pact file is then verified by product-service (ProductApiContractTest).
 * Matchers (types, regex) instead of fixed values keep the contract about shape, not data.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "product-service", pactVersion = PactSpecVersion.V3)
class ProductClientContractTest {

    private static final String EXISTING = "00000000-0000-0000-0000-000000000001";
    private static final String MISSING = "99999999-9999-9999-9999-999999999999";

    @Pact(consumer = "cart-service")
    RequestResponsePact existingProduct(PactDslWithProvider builder) {
        return builder
                .given("an active product " + EXISTING + " exists")
                .uponReceiving("a request for an existing product")
                .path("/api/products/" + EXISTING)
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(java.util.Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .uuid("id", UUID.fromString(EXISTING))
                        .stringType("name", "Wireless Headphones")
                        .decimalType("price", new BigDecimal("149.90"))
                        .stringMatcher("currency", "[A-Z]{3}", "EUR")
                        .booleanType("active", true))
                .toPact();
    }

    @Pact(consumer = "cart-service")
    RequestResponsePact missingProduct(PactDslWithProvider builder) {
        return builder
                .given("no product " + MISSING + " exists")
                .uponReceiving("a request for a product that does not exist")
                .path("/api/products/" + MISSING)
                .method("GET")
                .willRespondWith()
                .status(404)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "existingProduct")
    void readsTheFieldsCartNeeds(MockServer productService) {
        ProductView product = client(productService).get(UUID.fromString(EXISTING));

        assertThat(product.id()).isEqualTo(UUID.fromString(EXISTING));
        assertThat(product.price()).isEqualByComparingTo("149.90");
        assertThat(product.currency()).isEqualTo("EUR");
        assertThat(product.active()).isTrue();
    }

    @Test
    @PactTestFor(pactMethod = "missingProduct")
    void translates404IntoProductNotFound(MockServer productService) {
        assertThatThrownBy(() -> client(productService).get(UUID.fromString(MISSING)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    private static ProductClient client(MockServer server) {
        return new ProductClient(RestClient.builder(), server.getUrl(), Duration.ofSeconds(2), Duration.ofSeconds(2));
    }
}
