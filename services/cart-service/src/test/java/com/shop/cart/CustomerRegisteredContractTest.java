package com.shop.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.shop.cart.customers.CustomerRegisteredHandler;
import com.shop.cart.customers.KnownCustomer;
import com.shop.cart.customers.KnownCustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Contract layer (consumer side, async): the Kafka event is a contract too.
 * cart-service records the CustomerRegistered shape it can process; customer-service
 * verifies it (CustomerEventsContractTest). No broker is needed on either side.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "customer-service", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V3)
class CustomerRegisteredContractTest {

    private static final String ISO_INSTANT = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$";

    @Pact(consumer = "cart-service")
    MessagePact customerRegistered(MessagePactBuilder builder) {
        return builder
                .expectsToReceive("a CustomerRegistered event")
                .withMetadata(Map.of("contentType", "application/json"))
                .withContent(new PactDslJsonBody()
                        .uuid("eventId")
                        .stringValue("eventType", "CustomerRegistered")
                        .stringMatcher("occurredAt", ISO_INSTANT, "2026-10-22T10:00:00.123Z")
                        .uuid("customerId", UUID.fromString("0a1b2c3d-0000-4000-8000-000000000001"))
                        .stringType("email", "jane@example.com"))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "customerRegistered")
    void projectsTheCustomer(List<Message> messages) {
        KnownCustomerRepository repository = mock(KnownCustomerRepository.class);
        CustomerRegisteredHandler handler =
                new CustomerRegisteredHandler(repository, Jackson2ObjectMapperBuilder.json().build());

        handler.handle(messages.getFirst().contentsAsString());

        ArgumentCaptor<KnownCustomer> saved = ArgumentCaptor.forClass(KnownCustomer.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getCustomerId()).isEqualTo(UUID.fromString("0a1b2c3d-0000-4000-8000-000000000001"));
        assertThat(saved.getValue().getEmail()).isEqualTo("jane@example.com");
    }
}
