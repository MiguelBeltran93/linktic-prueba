package com.linktic.catalog.controller;

import com.linktic.catalog.dto.request.CreateItemRequest;
import com.linktic.catalog.dto.response.ApiData;
import com.linktic.catalog.dto.response.ApiResponse;
import com.linktic.catalog.dto.response.ItemRepresentation;
import com.linktic.catalog.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/items")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Catalog item management endpoints")
@SecurityRequirement(name = "X-API-KEY")
public class CatalogController {

    private final CatalogService catalogService;

    @Operation(summary = "Register a new catalog item")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApiData<ItemRepresentation>> register(@Valid @RequestBody CreateItemRequest request) {
        return catalogService.register(request);
    }

    @Operation(summary = "Get a catalog item by ID")
    @GetMapping("/{id}")
    public ApiResponse<ApiData<ItemRepresentation>> findById(@PathVariable Long id) {
        return catalogService.findById(id);
    }

    @Operation(summary = "List all catalog items")
    @GetMapping
    public ApiResponse<List<ApiData<ItemRepresentation>>> listAll() {
        return catalogService.listAll();
    }
}
