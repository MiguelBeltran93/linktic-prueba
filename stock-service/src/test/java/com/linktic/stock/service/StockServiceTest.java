package com.linktic.stock.service;

import com.linktic.stock.dto.request.PurchaseRequest;
import com.linktic.stock.dto.response.ApiData;
import com.linktic.stock.dto.response.ApiResponse;
import com.linktic.stock.dto.response.PurchaseRepresentation;
import com.linktic.stock.dto.response.StockRepresentation;
import com.linktic.stock.exception.InsufficientStockException;
import com.linktic.stock.gateway.ProductGateway;
import com.linktic.stock.messaging.StockEventPublisher;
import com.linktic.stock.model.PurchaseRecord;
import com.linktic.stock.model.StockEntry;
import com.linktic.stock.repository.PurchaseRepository;
import com.linktic.stock.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockService Unit Tests")
class StockServiceTest {

    @Mock private StockRepository stockRepository;
    @Mock private PurchaseRepository purchaseRepository;
    @Mock private ProductGateway productGateway;
    @Mock private StockEventPublisher eventPublisher;

    @InjectMocks
    private StockService stockService;

    private StockEntry sampleEntry;

    @BeforeEach
    void setUp() {
        sampleEntry = StockEntry.builder()
                .id(1L).productId(10L).quantity(100)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getStock - existing stock entry should return correct response")
    void getStock_whenEntryExists_shouldReturnStockResponse() {
        doNothing().when(productGateway).validateProduct(10L);
        when(stockRepository.findByProductId(10L)).thenReturn(Optional.of(sampleEntry));

        ApiResponse<ApiData<StockRepresentation>> response = stockService.getStock(10L);

        assertThat(response.getData().getType()).isEqualTo("stock-entries");
        assertThat(response.getData().getAttributes().getProductId()).isEqualTo(10L);
        assertThat(response.getData().getAttributes().getQuantity()).isEqualTo(100);
        verify(productGateway).validateProduct(10L);
        verify(stockRepository).findByProductId(10L);
    }

    @Test
    @DisplayName("getStock - no existing entry should create one with qty 0")
    void getStock_whenNoEntry_shouldCreateWithZeroQuantity() {
        doNothing().when(productGateway).validateProduct(10L);
        when(stockRepository.findByProductId(10L)).thenReturn(Optional.empty());
        when(stockRepository.save(any())).thenReturn(
                StockEntry.builder().id(2L).productId(10L).quantity(0).build());

        ApiResponse<ApiData<StockRepresentation>> response = stockService.getStock(10L);

        assertThat(response.getData().getAttributes().getQuantity()).isEqualTo(0);
        verify(stockRepository).save(any(StockEntry.class));
    }

    @Test
    @DisplayName("processPurchase - sufficient stock should deduct and record purchase")
    void processPurchase_withSufficientStock_shouldDeductAndRecord() {
        PurchaseRequest request = PurchaseRequest.builder().productId(10L).units(30).build();
        PurchaseRecord savedRecord = PurchaseRecord.builder()
                .id(1L).productId(10L).units(30).purchasedAt(LocalDateTime.now()).build();

        when(stockRepository.findByProductId(10L)).thenReturn(Optional.of(sampleEntry));
        when(stockRepository.save(any())).thenReturn(sampleEntry);
        when(purchaseRepository.save(any())).thenReturn(savedRecord);

        ApiResponse<ApiData<PurchaseRepresentation>> response = stockService.processPurchase(request);

        assertThat(response.getData().getType()).isEqualTo("purchase-records");
        assertThat(response.getData().getAttributes().getUnits()).isEqualTo(30);
        assertThat(response.getData().getAttributes().getRemainingStock()).isEqualTo(70);
        verify(eventPublisher).publishStockUpdated(eq(10L), eq(70), eq("PURCHASE"));
    }

    @Test
    @DisplayName("processPurchase - insufficient stock should throw InsufficientStockException")
    void processPurchase_withInsufficientStock_shouldThrow() {
        PurchaseRequest request = PurchaseRequest.builder().productId(10L).units(200).build();
        when(stockRepository.findByProductId(10L)).thenReturn(Optional.of(sampleEntry));

        assertThatThrownBy(() -> stockService.processPurchase(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("10")
                .hasMessageContaining("100");

        verify(purchaseRepository, never()).save(any());
        verify(eventPublisher, never()).publishStockUpdated(anyLong(), anyInt(), any());
    }

    @Test
    @DisplayName("processPurchase - no existing entry should create with qty 0 then fail")
    void processPurchase_whenNoEntry_createsZeroEntryThenFails() {
        PurchaseRequest request = PurchaseRequest.builder().productId(10L).units(1).build();
        StockEntry zeroEntry = StockEntry.builder().id(3L).productId(10L).quantity(0).build();

        when(stockRepository.findByProductId(10L)).thenReturn(Optional.empty());
        when(stockRepository.save(any())).thenReturn(zeroEntry);

        assertThatThrownBy(() -> stockService.processPurchase(request))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("adjustStock - should set new quantity and publish event")
    void adjustStock_shouldSetQuantityAndPublish() {
        when(stockRepository.findByProductId(10L)).thenReturn(Optional.of(sampleEntry));
        when(stockRepository.save(any())).thenReturn(sampleEntry);

        ApiResponse<ApiData<StockRepresentation>> response = stockService.adjustStock(10L, 250);

        assertThat(sampleEntry.getQuantity()).isEqualTo(250);
        verify(eventPublisher).publishStockUpdated(eq(10L), eq(250), eq("ADJUSTMENT"));
    }

    @Test
    @DisplayName("adjustStock - no existing entry should create then adjust")
    void adjustStock_whenNoEntry_shouldCreateAndAdjust() {
        StockEntry newEntry = StockEntry.builder().id(4L).productId(10L).quantity(0).build();
        when(stockRepository.findByProductId(10L)).thenReturn(Optional.empty());
        when(stockRepository.save(any())).thenReturn(newEntry);

        stockService.adjustStock(10L, 50);

        verify(eventPublisher).publishStockUpdated(eq(10L), eq(50), eq("ADJUSTMENT"));
    }
}
