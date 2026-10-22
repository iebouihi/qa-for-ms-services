package com.shop.product;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    static final int MAX_PAGE_SIZE = 50;

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    /** Browsing only shows active products; a blank query lists the catalog. */
    public Page<Product> browse(String query, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by("name"));
        if (query == null || query.isBlank()) {
            return repository.findByActiveTrue(pageRequest);
        }
        return repository.findByActiveTrueAndNameContainingIgnoreCase(query.trim(), pageRequest);
    }

    /** Direct lookup also returns inactive products: callers (e.g. cart) decide what to do with them. */
    public Product get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(UUID id) {
            super("Product " + id + " not found");
        }
    }
}
