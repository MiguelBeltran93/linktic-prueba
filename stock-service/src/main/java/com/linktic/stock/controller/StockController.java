package com.linktic.stock.controller;

import com.linktic.stock.dto.request.PurchaseRequest;
import com.linktic.stock.dto.response.ApiData;
import com.linktic.stock.dto.response.ApiResponse;
import com.linktic.stock.dto.response.PurchaseRepresentation;
import com.linktic.stock.dto.response.StockRepresentation;
import com.linktic.stock.service.StockService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Inventory management endpoints")
@SecurityRequirement(name = "X-API-KEY")
public class StockController {

    private final StockService stockService;

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ApiData<StockRepresentation>>> getStock(
            @PathVariable Long productId) {
        return ResponseEntity.ok(stockService.getStock(productId));
    }

    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApiData<PurchaseRepresentation>> purchase(
            @Valid @RequestBody PurchaseRequest request) {
        return stockService.processPurchase(request);
    }

    @PutMapping("/{productId}/adjust")
    public ApiResponse<ApiData<StockRepresentation>> adjust(
            @PathVariable Long productId,
            @RequestParam @Min(0) Integer quantity) {
        return stockService.adjustStock(productId, quantity);
    }
}
