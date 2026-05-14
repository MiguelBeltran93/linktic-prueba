package com.linktic.stock.service;

import com.linktic.stock.dto.request.PurchaseRequest;
import com.linktic.stock.dto.response.ApiData;
import com.linktic.stock.dto.response.ApiResponse;
import com.linktic.stock.dto.response.PurchaseRepresentation;
import com.linktic.stock.dto.response.StockRepresentation;
import com.linktic.stock.exception.InsufficientStockException;
import com.linktic.stock.gateway.ProductGateway;
import com.linktic.stock.mapper.StockMapper;
import com.linktic.stock.messaging.StockEventPublisher;
import com.linktic.stock.model.PurchaseRecord;
import com.linktic.stock.model.StockEntry;
import com.linktic.stock.repository.PurchaseRepository;
import com.linktic.stock.repository.StockRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockRepository stockRepository;
    private final PurchaseRepository purchaseRepository;
    private final ProductGateway productGateway;
    private final StockEventPublisher eventPublisher;

    public ApiResponse<ApiData<StockRepresentation>> getStock(Long productId) {
        productGateway.validateProduct(productId);
        StockEntry entry = stockRepository.findByProductId(productId)
                .orElseGet(() -> stockRepository.save(
                        StockEntry.builder().productId(productId).quantity(0).build()));
        log.debug("getStock productId={} qty={}", productId, entry.getQuantity());
        return StockMapper.toStockResponse(entry);
    }

    @Transactional
    public ApiResponse<ApiData<PurchaseRepresentation>> processPurchase(PurchaseRequest request) {
        StockEntry entry = stockRepository.findByProductId(request.getProductId())
                .orElseGet(() -> stockRepository.save(
                        StockEntry.builder().productId(request.getProductId()).quantity(0).build()));

        if (entry.getQuantity() < request.getUnits()) {
            throw new InsufficientStockException(request.getProductId(), entry.getQuantity());
        }
        entry.setQuantity(entry.getQuantity() - request.getUnits());
        stockRepository.save(entry);

        PurchaseRecord record = purchaseRepository.save(PurchaseRecord.builder()
                .productId(request.getProductId())
                .units(request.getUnits())
                .build());

        eventPublisher.publishStockUpdated(entry.getProductId(), entry.getQuantity(), "PURCHASE");
        log.info("Purchase processed: productId={} units={} remaining={}",
                request.getProductId(), request.getUnits(), entry.getQuantity());
        return StockMapper.toPurchaseResponse(record, entry.getQuantity());
    }

    @Transactional
    public ApiResponse<ApiData<StockRepresentation>> adjustStock(Long productId, Integer quantity) {
        StockEntry entry = stockRepository.findByProductId(productId)
                .orElseGet(() -> stockRepository.save(
                        StockEntry.builder().productId(productId).quantity(0).build()));
        entry.setQuantity(quantity);
        stockRepository.save(entry);
        eventPublisher.publishStockUpdated(productId, quantity, "ADJUSTMENT");
        log.info("Stock adjusted: productId={} newQty={}", productId, quantity);
        return StockMapper.toStockResponse(entry);
    }
}
