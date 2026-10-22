package com.shop.customer;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.shop.customer.CustomerApi.RegisterCustomerRequest;
import com.shop.customer.events.CustomerRegisteredEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Autowired // two constructors: the other one injects a fixed Clock in unit tests
    public CustomerService(CustomerRepository repository, ApplicationEventPublisher events) {
        this(repository, events, Clock.systemUTC());
    }

    CustomerService(CustomerRepository repository, ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public Customer register(RegisterCustomerRequest request) {
        String email = request.email().trim().toLowerCase();
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }
        Customer customer = repository.save(new Customer(
                UUID.randomUUID(), email, request.firstName().trim(), request.lastName().trim(), Instant.now(clock)));
        // Published to Kafka only after the transaction commits (see CustomerEventPublisher).
        events.publishEvent(CustomerRegisteredEvent.from(customer, Instant.now(clock)));
        return customer;
    }

    @Transactional(readOnly = true)
    public Customer get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
    }

    public static class DuplicateEmailException extends RuntimeException {
        public DuplicateEmailException(String email) {
            super("A customer with email " + email + " already exists");
        }
    }

    public static class CustomerNotFoundException extends RuntimeException {
        public CustomerNotFoundException(UUID id) {
            super("Customer " + id + " not found");
        }
    }
}
