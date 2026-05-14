package com.linktic.stock.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseRepresentation {
    private Long purchaseId;
    private Long productId;
    private Integer units;
    private Integer remainingStock;
    private LocalDateTime purchasedAt;
}
