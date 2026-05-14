package com.linktic.stock.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linktic.stock.config.StockAuthFilter;
import com.linktic.stock.dto.request.PurchaseRequest;
import com.linktic.stock.dto.response.ApiData;
import com.linktic.stock.dto.response.ApiResponse;
import com.linktic.stock.dto.response.PurchaseRepresentation;
import com.linktic.stock.dto.response.StockRepresentation;
import com.linktic.stock.exception.InsufficientStockException;
import com.linktic.stock.exception.ProductNotFoundException;
import com.linktic.stock.exception.StockExceptionHandler;
import com.linktic.stock.service.StockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = StockController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = StockAuthFilter.class)
)
@Import(StockExceptionHandler.class)
@DisplayName("StockController Unit Tests")
class StockControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private StockService stockService;

    private static final String BASE_URL = "/api/v1/stock";

    private ApiResponse<ApiData<StockRepresentation>> stockResponse(Long productId, Integer qty) {
        return ApiResponse.<ApiData<StockRepresentation>>builder()
                .data(ApiData.<StockRepresentation>builder()
                        .id("1").type("stock-entries")
                        .attributes(StockRepresentation.builder()
                                .productId(productId).quantity(qty)
                                .updatedAt(LocalDateTime.now()).build())
                        .build())
                .build();
    }

    private ApiResponse<ApiData<PurchaseRepresentation>> purchaseResponse(Long productId, int units, int remaining) {
        return ApiResponse.<ApiData<PurchaseRepresentation>>builder()
                .data(ApiData.<PurchaseRepresentation>builder()
                        .id("1").type("purchase-records")
                        .attributes(PurchaseRepresentation.builder()
                                .purchaseId(1L).productId(productId)
                                .units(units).remainingStock(remaining).build())
                        .build())
                .build();
    }

    @Test
    @DisplayName("GET /{productId} - should return 200 with stock info")
    void getStock_shouldReturn200() throws Exception {
        when(stockService.getStock(10L)).thenReturn(stockResponse(10L, 100));

        mockMvc.perform(get(BASE_URL + "/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("stock-entries"))
                .andExpect(jsonPath("$.data.attributes.productId").value(10))
                .andExpect(jsonPath("$.data.attributes.quantity").value(100));
    }

    @Test
    @DisplayName("GET /{productId} - product not found should return 404")
    void getStock_productNotFound_shouldReturn404() throws Exception {
        when(stockService.getStock(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].status").value("404"))
                .andExpect(jsonPath("$.errors[0].title").value("Product Not Found"));
    }

    @Test
    @DisplayName("POST /purchase - valid request should return 201")
    void purchase_withValidRequest_shouldReturn201() throws Exception {
        PurchaseRequest request = PurchaseRequest.builder().productId(10L).units(5).build();
        ApiResponse<ApiData<PurchaseRepresentation>> resp = ApiResponse.<ApiData<PurchaseRepresentation>>builder()
                .data(ApiData.<PurchaseRepresentation>builder()
                        .id("1").type("purchase-records")
                        .attributes(PurchaseRepresentation.builder()
                                .purchaseId(1L).productId(10L).units(5).remainingStock(95).build())
                        .build())
                .build();

        when(stockService.processPurchase(any())).thenReturn(resp);

        mockMvc.perform(post(BASE_URL + "/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("purchase-records"))
                .andExpect(jsonPath("$.data.attributes.units").value(5))
                .andExpect(jsonPath("$.data.attributes.remainingStock").value(95));
    }

    @Test
    @DisplayName("POST /purchase - invalid request (missing productId) should return 422")
    void purchase_withMissingProductId_shouldReturn422() throws Exception {
        PurchaseRequest request = PurchaseRequest.builder().units(5).build();

        mockMvc.perform(post(BASE_URL + "/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].status").value("422"));
    }

    @Test
    @DisplayName("POST /purchase - insufficient stock should return 422")
    void purchase_withInsufficientStock_shouldReturn422() throws Exception {
        PurchaseRequest request = PurchaseRequest.builder().productId(10L).units(999).build();
        when(stockService.processPurchase(any())).thenThrow(new InsufficientStockException(10L, 50));

        mockMvc.perform(post(BASE_URL + "/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].status").value("422"))
                .andExpect(jsonPath("$.errors[0].title").value("Insufficient Stock"));
    }

    @Test
    @DisplayName("PUT /{productId}/adjust - should return 200 with updated stock")
    void adjust_shouldReturn200() throws Exception {
        when(stockService.adjustStock(eq(10L), eq(200))).thenReturn(stockResponse(10L, 200));

        mockMvc.perform(put(BASE_URL + "/10/adjust").param("quantity", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributes.quantity").value(200));
    }

    @Test
    @DisplayName("PUT /{productId}/adjust - negative quantity should return 500")
    void adjust_withNegativeQuantity_shouldFail() throws Exception {
        mockMvc.perform(put(BASE_URL + "/10/adjust").param("quantity", "-1"))
                .andExpect(status().is5xxServerError());
    }
}
