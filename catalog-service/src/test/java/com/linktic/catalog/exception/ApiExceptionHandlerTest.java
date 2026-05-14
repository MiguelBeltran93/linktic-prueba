package com.linktic.catalog.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiExceptionHandler Unit Tests")
class ApiExceptionHandlerTest {

    private ApiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ApiExceptionHandler();
    }

    @Test
    @DisplayName("handleItemNotFound - should return 404 with correct JSON API error structure")
    void handleItemNotFound_shouldReturn404() {
        ItemNotFoundException ex = new ItemNotFoundException(42L);

        ResponseEntity<ErrorResponse> response = handler.handleItemNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).hasSize(1);
        assertThat(response.getBody().getErrors().get(0).getStatus()).isEqualTo("404");
        assertThat(response.getBody().getErrors().get(0).getTitle()).isEqualTo("Item Not Found");
        assertThat(response.getBody().getErrors().get(0).getDetail()).contains("42");
    }

    @Test
    @DisplayName("handleGeneral - should return 500 with generic error message")
    void handleGeneral_shouldReturn500() {
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<ErrorResponse> response = handler.handleGeneral(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getErrors().get(0).getStatus()).isEqualTo("500");
        assertThat(response.getBody().getErrors().get(0).getTitle()).isEqualTo("Internal Server Error");
    }

    @Test
    @DisplayName("handleItemNotFound - error detail should contain item id")
    void handleItemNotFound_detailShouldContainId() {
        Long itemId = 123L;
        ItemNotFoundException ex = new ItemNotFoundException(itemId);

        ResponseEntity<ErrorResponse> response = handler.handleItemNotFound(ex);

        assertThat(response.getBody().getErrors().get(0).getDetail())
                .contains(String.valueOf(itemId));
    }
}
