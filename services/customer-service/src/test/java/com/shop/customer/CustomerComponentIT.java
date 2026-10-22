package com.shop.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.observability.CorrelationId;
import com.shop.observability.KafkaCorrelation;
import com.shop.testsupport.KafkaTestConsumer;
import com.shop.testsupport.ProductionLikeContainers;
import com.shop.testsupport.TestData;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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

/**
 * Component layer: the real service with a real PostgreSQL and a real Kafka (Testcontainers),
 * exercised through its HTTP API. Catches what mocks cannot: SQL dialect, migrations,
 * unique indexes, serialization, and the event actually landing on the topic.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ProductionLikeContainers.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerComponentIT {

    @Autowired
    TestRestTemplate http;
    @Autowired
    ObjectMapper json;
    /** Where Testcontainers' Kafka really is (the spring.kafka.* properties are not updated). */
    @Autowired
    KafkaConnectionDetails kafkaConnection;

    KafkaTestConsumer events;

    @BeforeAll
    void subscribe() {
        events = new KafkaTestConsumer(bootstrapServers(), "customer.registered");
    }

    private String bootstrapServers() {
        return String.join(",", kafkaConnection.getBootstrapServers());
    }

    @AfterAll
    void close() {
        events.close();
    }

    @Test
    void registrationPersistsCustomerAndPublishesEventWithCorrelationId() throws Exception {
        String email = TestData.uniqueEmail();
        HttpHeaders headers = new HttpHeaders();
        headers.set(CorrelationId.HEADER, "it-registration-001");

        ResponseEntity<JsonNode> created = http.postForEntity("/api/customers",
                new HttpEntity<>(Map.of("email", email, "firstName", "Jane", "lastName", "Doe"), headers),
                JsonNode.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String customerId = created.getBody().get("id").asText();
        assertThat(http.getForEntity("/api/customers/" + customerId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        ConsumerRecord<String, String> record = events.awaitRecordWithKey(customerId);
        JsonNode event = json.readTree(record.value());
        assertThat(event.get("eventType").asText()).isEqualTo("CustomerRegistered");
        assertThat(event.get("email").asText()).isEqualTo(email);
        assertThat(KafkaCorrelation.from(record.headers())).isEqualTo("it-registration-001");
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() {
        String email = TestData.uniqueEmail();
        http.postForEntity("/api/customers", Map.of("email", email, "firstName", "A", "lastName", "B"), JsonNode.class);

        ResponseEntity<JsonNode> duplicate = http.postForEntity("/api/customers",
                Map.of("email", email.toUpperCase(), "firstName", "A", "lastName", "B"), JsonNode.class);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("code").asText()).isEqualTo("EMAIL_ALREADY_REGISTERED");
    }

    @Test
    void invalidPayloadIsRejected() {
        ResponseEntity<JsonNode> response = http.postForEntity("/api/customers",
                Map.of("email", "not-an-email", "firstName", "", "lastName", "B"), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
