package com.linktic.stock.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdatedEvent {
    private Long productId;
    private Integer newQuantity;
    private String operation;
    private LocalDateTime occurredAt;
}
