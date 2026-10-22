package com.shop.cart;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.shop.observability.CorrelationId;
import com.shop.testsupport.KafkaTestConsumer;
import com.shop.testsupport.ProductionLikeContainers;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Component layer for cart-service, the most integrated service:
 * <ul>
 *   <li>real PostgreSQL and Kafka (Testcontainers),</li>
 *   <li>product-service virtualized with WireMock (we test cart, not product),</li>
 *   <li>customers arrive the way they do in production: as Kafka events.</li>
 * </ul>
 * Eventual consistency is handled by polling with a deadline (Awaitility), never sleep().
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ProductionLikeContainers.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CartComponentIT {

    /**
     * Started statically, before the Spring context is built (the context needs its URL).
     * Every test stubs its own product ids, so stubs never collide between tests.
     */
    static final WireMockServer productService = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        productService.start();
    }

    @DynamicPropertySource
    static void productServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("shop.clients.product.base-url", productService::baseUrl);
        registry.add("shop.clients.product.read-timeout", () -> "1s");
    }

    @Autowired
    TestRestTemplate http;
    @Autowired
    KafkaTemplate<String, String> kafka;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ObjectMapper json;
    /** Where Testcontainers' Kafka really is (the spring.kafka.* properties are not updated). */
    @Autowired
    KafkaConnectionDetails kafkaConnection;

    KafkaTestConsumer cartCreated;
    KafkaTestConsumer deadLetters;

    @BeforeAll
    void subscribe() {
        cartCreated = new KafkaTestConsumer(bootstrapServers(), "cart.created");
        deadLetters = new KafkaTestConsumer(bootstrapServers(), "customer.registered.DLT");
    }

    private String bootstrapServers() {
        return String.join(",", kafkaConnection.getBootstrapServers());
    }

    @AfterAll
    void close() {
        cartCreated.close();
        deadLetters.close();
        productService.stop();
    }

    @Test
    void createsCartWithPriceSnapshotAndPublishesCartCreated() throws Exception {
        UUID customerId = publishCustomerRegistered();
        UUID headphones = stubProduct("Wireless Headphones", "10.50", true);
        UUID mouse = stubProduct("Ergonomic Mouse", "3.25", true);

        ResponseEntity<JsonNode> response = createCartOnceCustomerIsKnown(customerId, "it-cart-001",
                List.of(Map.of("productId", headphones, "quantity", 2), Map.of("productId", mouse, "quantity", 1)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode cart = response.getBody();
        assertThat(cart.get("total").decimalValue()).isEqualByComparingTo("24.25");
        assertThat(cart.get("items")).hasSize(2);

        JsonNode event = json.readTree(cartCreated.awaitRecordWithKey(cart.get("id").asText()).value());
        assertThat(event.get("eventType").asText()).isEqualTo("CartCreated");
        assertThat(event.get("customerId").asText()).isEqualTo(customerId.toString());

        // Observability: the caller's correlation id reached the downstream service.
        productService.verify(getRequestedFor(urlEqualTo("/api/products/" + headphones))
                .withHeader(CorrelationId.HEADER, equalTo("it-cart-001")));
    }

    @Test
    void replayedCustomerEventIsIdempotent() throws Exception {
        UUID customerId = UUID.randomUUID();
        String event = customerRegisteredJson(customerId);

        kafka.send(new ProducerRecord<>("customer.registered", customerId.toString(), event)).get();
        kafka.send(new ProducerRecord<>("customer.registered", customerId.toString(), event)).get();

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(knownCustomerRows(customerId)).isEqualTo(1));
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(knownCustomerRows(customerId)).isEqualTo(1));
    }

    @Test
    void poisonMessageIsParkedOnDeadLetterTopicWithoutBlockingTheConsumer() throws Exception {
        String key = "poison-" + UUID.randomUUID();
        kafka.send(new ProducerRecord<>("customer.registered", key, "{not valid json")).get();

        assertThat(deadLetters.awaitRecordWithKey(key).value()).isEqualTo("{not valid json");

        // The partition is not blocked: the next valid customer is still processed.
        UUID next = publishCustomerRegistered();
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(knownCustomerRows(next)).isEqualTo(1));
    }

    @Test
    void unknownCustomerIsRejectedWithARetryableCode() {
        ResponseEntity<JsonNode> response = createCart(UUID.randomUUID(), "it-cart-002",
                List.of(Map.of("productId", UUID.randomUUID(), "quantity", 1)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().get("code").asText()).isEqualTo("CUSTOMER_UNKNOWN");
    }

    @Test
    void inactiveProductIsRejected() {
        UUID customerId = publishCustomerRegistered();
        UUID discontinued = stubProduct("USB-C Dock", "129.00", false);

        ResponseEntity<JsonNode> response = createCartOnceCustomerIsKnown(customerId, "it-cart-003",
                List.of(Map.of("productId", discontinued, "quantity", 1)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().get("code").asText()).isEqualTo("PRODUCT_UNAVAILABLE");
    }

    @Test
    void slowProductServiceFailsFastWith503() {
        UUID customerId = publishCustomerRegistered();
        UUID slow = UUID.randomUUID();
        productService.stubFor(get("/api/products/" + slow).willReturn(okJson("{}").withFixedDelay(3_000)));

        ResponseEntity<JsonNode> response = createCartOnceCustomerIsKnown(customerId, "it-cart-004",
                List.of(Map.of("productId", slow, "quantity", 1)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().get("code").asText()).isEqualTo("PRODUCT_SERVICE_UNAVAILABLE");
    }

    @Test
    void invalidQuantityIsRejected() {
        ResponseEntity<JsonNode> response = createCart(UUID.randomUUID(), "it-cart-005",
                List.of(Map.of("productId", UUID.randomUUID(), "quantity", 0)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---- helpers: each test creates its own data -------------------------------------------

    private UUID publishCustomerRegistered() {
        UUID customerId = UUID.randomUUID();
        try {
            kafka.send(new ProducerRecord<>("customer.registered", customerId.toString(),
                    customerRegisteredJson(customerId))).get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return customerId;
    }

    private String customerRegisteredJson(UUID customerId) throws Exception {
        return json.writeValueAsString(Map.of(
                "eventId", UUID.randomUUID(),
                "eventType", "CustomerRegistered",
                "occurredAt", Instant.now().toString(),
                "customerId", customerId,
                "email", "test-" + customerId + "@example.test",
                "firstName", "Test",
                "lastName", "Customer"));
    }

    private UUID stubProduct(String name, String price, boolean active) {
        UUID id = UUID.randomUUID();
        productService.stubFor(get("/api/products/" + id).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"id":"%s","sku":"SKU-%s","name":"%s","price":%s,"currency":"EUR","active":%s}
                        """.formatted(id, id, name, price, active))));
        return id;
    }

    /** Retries only while the customer event is still in flight; any other answer is final. */
    private ResponseEntity<JsonNode> createCartOnceCustomerIsKnown(UUID customerId, String correlationId,
                                                                   List<Map<String, Object>> items) {
        return await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(250))
                .until(() -> createCart(customerId, correlationId, items),
                        r -> !(r.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY
                                && "CUSTOMER_UNKNOWN".equals(r.getBody().get("code").asText())));
    }

    private ResponseEntity<JsonNode> createCart(UUID customerId, String correlationId, List<Map<String, Object>> items) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(CorrelationId.HEADER, correlationId);
        return http.postForEntity("/api/carts",
                new HttpEntity<>(Map.of("customerId", customerId, "items", items), headers), JsonNode.class);
    }

    private int knownCustomerRows(UUID customerId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM known_customer WHERE customer_id = ?", Integer.class,
                customerId);
    }
}
