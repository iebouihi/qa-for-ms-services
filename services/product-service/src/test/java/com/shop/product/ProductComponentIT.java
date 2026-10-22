package com.shop.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.shop.testsupport.ProductionLikeContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Component layer: real PostgreSQL, real Flyway migrations and the seeded reference
 * catalog, exercised through HTTP. No Kafka: product-service does not use it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ProductionLikeContainers.Postgres.class)
@ActiveProfiles("seed")
class ProductComponentIT {

    private static final String HEADPHONES = "00000000-0000-0000-0000-000000000001";
    private static final String DISCONTINUED_DOCK = "00000000-0000-0000-0000-000000000006";

    @Autowired
    TestRestTemplate http;

    @Test
    void browsingListsOnlyActiveProductsSortedByName() {
        JsonNode page = http.getForObject("/api/products", JsonNode.class);

        assertThat(page.get("totalElements").asInt()).isEqualTo(5);
        assertThat(page.get("content").findValuesAsText("name"))
                .isSorted()
                .doesNotContain("USB-C Dock");
    }

    @Test
    void searchIsCaseInsensitive() {
        JsonNode page = http.getForObject("/api/products?q=KEYboard", JsonNode.class);

        assertThat(page.get("content").findValuesAsText("sku")).containsExactly("SKU-KEYBOARD");
    }

    @Test
    void paginationIsStable() {
        JsonNode first = http.getForObject("/api/products?size=2&page=0", JsonNode.class);
        JsonNode second = http.getForObject("/api/products?size=2&page=1", JsonNode.class);

        assertThat(first.get("content")).hasSize(2);
        assertThat(second.get("content")).hasSize(2);
        assertThat(first.get("content").findValuesAsText("id"))
                .doesNotContainAnyElementsOf(second.get("content").findValuesAsText("id"));
    }

    @Test
    void productDetailsKeepPrecisionAndExposeInactiveFlag() {
        JsonNode headphones = http.getForObject("/api/products/" + HEADPHONES, JsonNode.class);
        JsonNode dock = http.getForObject("/api/products/" + DISCONTINUED_DOCK, JsonNode.class);

        assertThat(headphones.get("price").decimalValue()).isEqualByComparingTo("149.90");
        assertThat(dock.get("active").asBoolean()).isFalse();
    }

    @Test
    void unknownProductReturns404() {
        ResponseEntity<JsonNode> response =
                http.getForEntity("/api/products/12345678-1234-1234-1234-123456789012", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
