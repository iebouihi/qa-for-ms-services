package com.shop.customer;

import java.net.URI;
import java.util.UUID;

import com.shop.customer.CustomerApi.CustomerResponse;
import com.shop.customer.CustomerApi.RegisterCustomerRequest;
import com.shop.customer.CustomerService.CustomerNotFoundException;
import com.shop.customer.CustomerService.DuplicateEmailException;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody RegisterCustomerRequest request) {
        Customer customer = service.register(request);
        return ResponseEntity.created(URI.create("/api/customers/" + customer.getId()))
                .body(CustomerResponse.from(customer));
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable UUID id) {
        return CustomerResponse.from(service.get(id));
    }

    @ExceptionHandler({DuplicateEmailException.class, DataIntegrityViolationException.class})
    ProblemDetail duplicate(RuntimeException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email already registered");
        problem.setProperty("code", "EMAIL_ALREADY_REGISTERED");
        return problem;
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    ProblemDetail notFound(CustomerNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
