package com.shop.journeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigDecimal;
import java.time.Duration;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Journey 3 - create cart (crosses all 3 services and Kafka)")
class CreateCartJourney extends JourneySupport {

    @Test
    void registeredCustomerCanCreateACartWithCatalogPrices() {
        String customerId = registerCustomer("cart");
        String productId = api("cart").get("/api/products").then().statusCode(200).extract().path("content[0].id");
        BigDecimal price = new BigDecimal(api("cart").get("/api/products/{id}", productId)
                .then().extract().path("price").toString());

        String body = """
                {"customerId":"%s","items":[{"productId":"%s","quantity":2}]}
                """.formatted(customerId, productId);

        // The customer reaches cart-service through Kafka: poll with a deadline, never sleep.
        Response created = await("cart-service learns about the new customer")
                .atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500))
                .until(() -> api("cart").body(body).post("/api/carts"), r -> r.statusCode() != 422);

        created.then().statusCode(201);
        String cartId = created.path("id");
        BigDecimal total = new BigDecimal(created.path("total").toString());
        assertThat(total).isEqualByComparingTo(price.multiply(BigDecimal.valueOf(2)));

        api("cart").get("/api/carts/{id}", cartId)
                .then().statusCode(200)
                .body("customerId", equalTo(customerId))
                .body("items[0].productId", equalTo(productId));
    }

    @Test
    void unknownCustomerIsRejected() {
        String productId = api("cart-unknown").get("/api/products").then().extract().path("content[0].id");

        api("cart-unknown")
                .body("""
                        {"customerId":"00000000-0000-4000-8000-000000000000","items":[{"productId":"%s","quantity":1}]}
                        """.formatted(productId))
                .post("/api/carts")
                .then().statusCode(422)
                .body("code", equalTo("CUSTOMER_UNKNOWN"));
    }
}
