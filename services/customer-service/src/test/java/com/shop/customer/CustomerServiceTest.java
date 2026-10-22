package com.shop.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.shop.customer.CustomerApi.RegisterCustomerRequest;
import com.shop.customer.CustomerService.DuplicateEmailException;
import com.shop.customer.events.CustomerRegisteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** Unit layer: business rules only, every dependency mocked, runs in milliseconds. */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

    @Mock
    CustomerRepository repository;
    @Mock
    ApplicationEventPublisher events;

    CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(repository, events, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void registersCustomerWithNormalisedEmailAndPublishesEvent() {
        when(repository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer customer = service.register(new RegisterCustomerRequest("  Jane@Example.com ", "Jane", "Doe"));

        assertThat(customer.getEmail()).isEqualTo("jane@example.com");
        assertThat(customer.getCreatedAt()).isEqualTo(NOW);

        ArgumentCaptor<CustomerRegisteredEvent> event = ArgumentCaptor.forClass(CustomerRegisteredEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().customerId()).isEqualTo(customer.getId());
        assertThat(event.getValue().eventType()).isEqualTo("CustomerRegistered");
    }

    @Test
    void rejectsDuplicateEmailWithoutPublishing() {
        when(repository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterCustomerRequest("jane@example.com", "Jane", "Doe")))
                .isInstanceOf(DuplicateEmailException.class);

        verify(repository, never()).save(any());
        verify(events, never()).publishEvent(any(Object.class));
    }
}
