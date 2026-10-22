package com.shop.cart.customers;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KnownCustomerRepository extends JpaRepository<KnownCustomer, UUID> {
}
