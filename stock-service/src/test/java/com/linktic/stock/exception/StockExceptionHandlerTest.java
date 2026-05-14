package com.linktic.stock.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StockExceptionHandler Unit Tests")
class StockExceptionHandlerTest {

    private final StockExceptionHandler handler = new StockExceptionHandler();

    @Test
    @DisplayName("handleProductNotFound - should return 404 error response")
    void handleProductNotFound_shouldReturn404() {
        ProductNotFoundException ex = new ProductNotFoundException(42L);

        ErrorResponse response = handler.handleProductNotFound(ex);

        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0).getStatus()).isEqualTo("404");
        assertThat(response.getErrors().get(0).getTitle()).isEqualTo("Product Not Found");
        assertThat(response.getErrors().get(0).getDetail()).contains("42");
    }

    @Test
    @DisplayName("handleInsufficientStock - should return 422 error response")
    void handleInsufficientStock_shouldReturn422() {
        InsufficientStockException ex = new InsufficientStockException(10L, 5);

        ErrorResponse response = handler.handleInsufficientStock(ex);

        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0).getStatus()).isEqualTo("422");
        assertThat(response.getErrors().get(0).getTitle()).isEqualTo("Insufficient Stock");
        assertThat(response.getErrors().get(0).getDetail()).contains("10").contains("5");
    }

    @Test
    @DisplayName("handleGeneral - should return 500 error response")
    void handleGeneral_shouldReturn500() {
        Exception ex = new RuntimeException("Unexpected failure");

        ErrorResponse response = handler.handleGeneral(ex);

        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0).getStatus()).isEqualTo("500");
        assertThat(response.getErrors().get(0).getTitle()).isEqualTo("Internal Server Error");
    }

    @Test
    @DisplayName("ProductNotFoundException - message should contain product id")
    void productNotFoundException_shouldContainId() {
        ProductNotFoundException ex = new ProductNotFoundException(99L);
        assertThat(ex.getMessage()).contains("99");
    }

    @Test
    @DisplayName("InsufficientStockException - message should contain productId and available")
    void insufficientStockException_shouldContainDetails() {
        InsufficientStockException ex = new InsufficientStockException(5L, 3);
        assertThat(ex.getMessage()).contains("5").contains("3");
    }

    @Test
    @DisplayName("ErrorResponse builder - should construct correct structure")
    void errorResponse_builderShouldWork() {
        ErrorResponse response = ErrorResponse.builder()
                .errors(List.of(ErrorResponse.ApiError.builder()
                        .status("404").title("Not Found").detail("missing").build()))
                .build();

        assertThat(response.getErrors().get(0).getStatus()).isEqualTo("404");
        assertThat(response.getErrors().get(0).getTitle()).isEqualTo("Not Found");
    }
}
