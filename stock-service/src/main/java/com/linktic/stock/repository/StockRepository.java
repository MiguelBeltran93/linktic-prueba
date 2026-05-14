package com.linktic.stock.repository;

import com.linktic.stock.model.StockEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<StockEntry, Long> {
    Optional<StockEntry> findByProductId(Long productId);
}
