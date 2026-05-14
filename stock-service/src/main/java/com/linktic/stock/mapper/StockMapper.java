package com.linktic.stock.mapper;

import com.linktic.stock.dto.response.*;
import com.linktic.stock.model.PurchaseRecord;
import com.linktic.stock.model.StockEntry;

public class StockMapper {

    private StockMapper() {}

    private static final String STOCK_TYPE    = "stock-entries";
    private static final String PURCHASE_TYPE = "purchase-records";

    public static ApiResponse<ApiData<StockRepresentation>> toStockResponse(StockEntry e) {
        return ApiResponse.<ApiData<StockRepresentation>>builder()
                .data(ApiData.<StockRepresentation>builder()
                        .id(String.valueOf(e.getId()))
                        .type(STOCK_TYPE)
                        .attributes(StockRepresentation.builder()
                                .productId(e.getProductId())
                                .quantity(e.getQuantity())
                                .updatedAt(e.getUpdatedAt())
                                .build())
                        .build())
                .build();
    }

    public static ApiResponse<ApiData<PurchaseRepresentation>> toPurchaseResponse(
            PurchaseRecord r, Integer remaining) {
        return ApiResponse.<ApiData<PurchaseRepresentation>>builder()
                .data(ApiData.<PurchaseRepresentation>builder()
                        .id(String.valueOf(r.getId()))
                        .type(PURCHASE_TYPE)
                        .attributes(PurchaseRepresentation.builder()
                                .purchaseId(r.getId())
                                .productId(r.getProductId())
                                .units(r.getUnits())
                                .remainingStock(remaining)
                                .purchasedAt(r.getPurchasedAt())
                                .build())
                        .build())
                .build();
    }
}
