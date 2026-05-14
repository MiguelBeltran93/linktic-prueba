package com.linktic.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to register a new catalog item")
public class CreateItemRequest {

    @NotBlank(message = "Name is required")
    @Schema(description = "Item name", example = "Laptop Pro")
    private String name;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be a positive value")
    @Schema(description = "Item price in USD", example = "999.99")
    private Double price;

    @Schema(description = "Optional item description", example = "High performance laptop")
    private String description;
}
