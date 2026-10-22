package com.shop.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

/** Public API payloads of the product service (documented in contracts/openapi/product-api.yaml). */
public final class ProductApi {

    private ProductApi() {
    }

    public record ProductResponse(UUID id, String sku, String name, String description, BigDecimal price,
                                  String currency, boolean active) {

        static ProductResponse from(Product p) {
            return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getPrice(),
                    p.getCurrency(), p.isActive());
        }
    }

    public record ProductPage(List<ProductResponse> content, int page, int size, long totalElements) {

        static ProductPage from(Page<Product> page) {
            return new ProductPage(page.map(ProductResponse::from).getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements());
        }
    }
}
