package com.shop.customer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import au.com.dius.pact.provider.MessageAndMetadata;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.customer.events.CustomerRegisteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Contract layer (provider side, async): proves that the event this service really
 * produces still satisfies what cart-service expects. Runs in CI on every PR, no Kafka needed.
 * If someone renames {@code customerId}, this test fails here, before anything is deployed.
 */
@Provider("customer-service")
@PactFolder("../../contracts/pacts") // written by cart-service's consumer tests (a Pact Broker in a real setup)
class CustomerEventsContractTest {

    private final ObjectMapper json = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void target(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget());
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyContracts(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @PactVerifyProvider("a CustomerRegistered event")
    MessageAndMetadata customerRegistered() throws Exception {
        Customer customer = new Customer(UUID.randomUUID(), "jane@example.com", "Jane", "Doe", Instant.now());
        String body = json.writeValueAsString(CustomerRegisteredEvent.from(customer, Instant.now()));
        return new MessageAndMetadata(body.getBytes(StandardCharsets.UTF_8), Map.of("contentType", "application/json"));
    }
}
