package com.linktic.stock.repository;

import com.linktic.stock.model.PurchaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<PurchaseRecord, Long> {
}
