package com.shop.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.shop.product.ProductService.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository repository;
    @InjectMocks
    ProductService service;

    @ParameterizedTest(name = "requested size {0} -> {1}")
    @CsvSource({"0, 1", "-5, 1", "20, 20", "50, 50", "500, 50"})
    void pageSizeIsClampedToProtectTheDatabase(int requested, int expected) {
        assertThat(ProductService.clampSize(requested)).isEqualTo(expected);
    }

    @Test
    void blankQueryBrowsesActiveCatalogSortedByName() {
        when(repository.findByActiveTrue(any())).thenReturn(Page.empty());

        service.browse("  ", 0, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByActiveTrue(pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("name")).isNotNull();
    }

    @Test
    void searchIsTrimmedAndCaseInsensitive() {
        when(repository.findByActiveTrueAndNameContainingIgnoreCase(eq("keyboard"), any())).thenReturn(Page.empty());

        service.browse(" keyboard ", 0, 20);

        verify(repository).findByActiveTrueAndNameContainingIgnoreCase(eq("keyboard"), any());
    }

    @Test
    void unknownProductRaisesNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(ProductNotFoundException.class);
    }
}
