package com.linktic.catalog.service;

import com.linktic.catalog.dto.request.CreateItemRequest;
import com.linktic.catalog.dto.response.ApiData;
import com.linktic.catalog.dto.response.ApiResponse;
import com.linktic.catalog.dto.response.ItemRepresentation;
import com.linktic.catalog.exception.ItemNotFoundException;
import com.linktic.catalog.model.CatalogItem;
import com.linktic.catalog.repository.CatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogService Unit Tests")
class CatalogServiceTest {

    @Mock
    private CatalogRepository catalogRepository;

    @InjectMocks
    private CatalogService catalogService;

    private CatalogItem sampleItem;
    private CreateItemRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleItem = CatalogItem.builder()
                .id(1L)
                .name("Laptop Pro")
                .price(999.99)
                .description("High performance laptop")
                .build();

        sampleRequest = CreateItemRequest.builder()
                .name("Laptop Pro")
                .price(999.99)
                .description("High performance laptop")
                .build();
    }

    @Test
    @DisplayName("register - should save item and return JSON API response")
    void register_shouldSaveItemAndReturnApiResponse() {
        when(catalogRepository.save(any(CatalogItem.class))).thenReturn(sampleItem);

        ApiResponse<ApiData<ItemRepresentation>> response = catalogService.register(sampleRequest);

        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getId()).isEqualTo("1");
        assertThat(response.getData().getType()).isEqualTo("catalog-items");
        assertThat(response.getData().getAttributes().getName()).isEqualTo("Laptop Pro");
        assertThat(response.getData().getAttributes().getPrice()).isEqualTo(999.99);
        verify(catalogRepository, times(1)).save(any(CatalogItem.class));
    }

    @Test
    @DisplayName("findById - when item exists should return correct response")
    void findById_whenItemExists_shouldReturnApiResponse() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(sampleItem));

        ApiResponse<ApiData<ItemRepresentation>> response = catalogService.findById(1L);

        assertThat(response.getData().getId()).isEqualTo("1");
        assertThat(response.getData().getAttributes().getName()).isEqualTo("Laptop Pro");
        verify(catalogRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("findById - when item not found should throw ItemNotFoundException")
    void findById_whenItemNotFound_shouldThrowItemNotFoundException() {
        when(catalogRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.findById(99L))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("99");

        verify(catalogRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("listAll - should return all items wrapped in JSON API format")
    void listAll_shouldReturnAllItems() {
        when(catalogRepository.findAll()).thenReturn(List.of(sampleItem));

        ApiResponse<List<ApiData<ItemRepresentation>>> response = catalogService.listAll();

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getType()).isEqualTo("catalog-items");
        assertThat(response.getData().get(0).getAttributes().getName()).isEqualTo("Laptop Pro");
        verify(catalogRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("listAll - when no items should return empty list")
    void listAll_whenNoItems_shouldReturnEmptyList() {
        when(catalogRepository.findAll()).thenReturn(List.of());

        ApiResponse<List<ApiData<ItemRepresentation>>> response = catalogService.listAll();

        assertThat(response.getData()).isEmpty();
    }

    @Test
    @DisplayName("register - should map all fields correctly to entity")
    void register_shouldMapAllFieldsCorrectly() {
        when(catalogRepository.save(any(CatalogItem.class))).thenReturn(sampleItem);

        catalogService.register(sampleRequest);

        verify(catalogRepository).save(argThat(item ->
                item.getName().equals("Laptop Pro") &&
                item.getPrice().equals(999.99) &&
                item.getDescription().equals("High performance laptop")
        ));
    }
}
