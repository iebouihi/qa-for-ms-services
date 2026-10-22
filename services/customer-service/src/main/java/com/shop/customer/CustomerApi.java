package com.shop.customer;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Public API payloads of the customer service. */
public final class CustomerApi {

    private CustomerApi() {
    }

    public record RegisterCustomerRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName) {
    }

    public record CustomerResponse(UUID id, String email, String firstName, String lastName, Instant createdAt) {

        static CustomerResponse from(Customer c) {
            return new CustomerResponse(c.getId(), c.getEmail(), c.getFirstName(), c.getLastName(), c.getCreatedAt());
        }
    }
}
