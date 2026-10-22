package com.shop.product;

import java.util.UUID;

import com.shop.product.ProductApi.ProductPage;
import com.shop.product.ProductApi.ProductResponse;
import com.shop.product.ProductService.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public ProductPage browse(@RequestParam(name = "q", required = false) String query,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size) {
        return ProductPage.from(service.browse(query, page, size));
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable UUID id) {
        return ProductResponse.from(service.get(id));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail notFound(ProductNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
