package com.shop.product;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.shop.product.ProductService.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contract layer (provider side, HTTP): replays every interaction cart-service recorded
 * against the real controller (web layer only, no database), with each provider state
 * set up through a mocked service. Fast, deterministic, no environment needed.
 */
@WebMvcTest(ProductController.class)
@Provider("product-service")
@PactFolder("../../contracts/pacts") // written by cart-service's consumer tests (a Pact Broker in a real setup)
class ProductApiContractTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    ProductService productService;

    @BeforeEach
    void target(PactVerificationContext context) {
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyContracts(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("an active product 00000000-0000-0000-0000-000000000001 exists")
    void activeProductExists() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(productService.get(id)).thenReturn(new Product(id, "SKU-HEADPHONES", "Wireless Headphones",
                "Over-ear", new BigDecimal("149.90"), "EUR", true));
    }

    @State("no product 99999999-9999-9999-9999-999999999999 exists")
    void productDoesNotExist() {
        UUID id = UUID.fromString("99999999-9999-9999-9999-999999999999");
        when(productService.get(id)).thenThrow(new ProductNotFoundException(id));
    }
}
