package com.linktic.stock.messaging;

import com.linktic.stock.config.BrokerConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishStockUpdated(Long productId, Integer qty, String operation) {
        StockUpdatedEvent event = StockUpdatedEvent.builder()
                .productId(productId)
                .newQuantity(qty)
                .operation(operation)
                .occurredAt(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend(
                BrokerConfiguration.STOCK_EXCHANGE,
                BrokerConfiguration.STOCK_ROUTING_KEY,
                event);
        log.info("stock.updated published: productId={} qty={} op={}", productId, qty, operation);
    }
}
