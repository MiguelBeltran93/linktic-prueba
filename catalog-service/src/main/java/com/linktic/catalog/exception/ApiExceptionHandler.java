package com.linktic.catalog.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleItemNotFound(ItemNotFoundException ex) {
        log.warn("Catalog item not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError("404", "Item Not Found", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.ApiError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ErrorResponse.ApiError.builder()
                        .status("422")
                        .title("Validation Error")
                        .detail(fe.getField() + ": " + fe.getDefaultMessage())
                        .build())
                .toList();
        log.warn("Validation error: {}", errors);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.builder().errors(errors).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError("500", "Internal Server Error", "An unexpected error occurred"));
    }

    private ErrorResponse buildError(String status, String title, String detail) {
        return ErrorResponse.builder()
                .errors(List.of(ErrorResponse.ApiError.builder()
                        .status(status)
                        .title(title)
                        .detail(detail)
                        .build()))
                .build();
    }
}
