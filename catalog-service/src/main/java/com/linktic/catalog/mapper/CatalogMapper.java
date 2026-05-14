package com.linktic.catalog.mapper;

import com.linktic.catalog.dto.request.CreateItemRequest;
import com.linktic.catalog.dto.response.ApiData;
import com.linktic.catalog.dto.response.ApiResponse;
import com.linktic.catalog.dto.response.ItemRepresentation;
import com.linktic.catalog.model.CatalogItem;

import java.util.List;

public class CatalogMapper {

    private static final String ITEM_TYPE = "catalog-items";

    private CatalogMapper() {
    }

    public static CatalogItem toEntity(CreateItemRequest request) {
        return CatalogItem.builder()
                .name(request.getName())
                .price(request.getPrice())
                .description(request.getDescription())
                .build();
    }

    public static ItemRepresentation toRepresentation(CatalogItem item) {
        return ItemRepresentation.builder()
                .id(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .description(item.getDescription())
                .createdAt(item.getCreatedAt())
                .build();
    }

    public static ApiResponse<ApiData<ItemRepresentation>> toSingleResponse(CatalogItem item) {
        return ApiResponse.<ApiData<ItemRepresentation>>builder()
                .data(ApiData.<ItemRepresentation>builder()
                        .id(String.valueOf(item.getId()))
                        .type(ITEM_TYPE)
                        .attributes(toRepresentation(item))
                        .build())
                .build();
    }

    public static ApiResponse<List<ApiData<ItemRepresentation>>> toListResponse(List<CatalogItem> items) {
        List<ApiData<ItemRepresentation>> dataList = items.stream()
                .map(item -> ApiData.<ItemRepresentation>builder()
                        .id(String.valueOf(item.getId()))
                        .type(ITEM_TYPE)
                        .attributes(toRepresentation(item))
                        .build())
                .toList();
        return ApiResponse.<List<ApiData<ItemRepresentation>>>builder()
                .data(dataList)
                .build();
    }
}
