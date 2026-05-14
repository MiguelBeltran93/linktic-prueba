package com.linktic.catalog.repository;

import com.linktic.catalog.model.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogRepository extends JpaRepository<CatalogItem, Long> {
}
