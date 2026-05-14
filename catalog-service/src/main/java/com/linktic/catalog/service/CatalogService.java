package com.linktic.catalog.service;

import com.linktic.catalog.dto.request.CreateItemRequest;
import com.linktic.catalog.dto.response.ApiData;
import com.linktic.catalog.dto.response.ApiResponse;
import com.linktic.catalog.dto.response.ItemRepresentation;
import com.linktic.catalog.exception.ItemNotFoundException;
import com.linktic.catalog.mapper.CatalogMapper;
import com.linktic.catalog.model.CatalogItem;
import com.linktic.catalog.repository.CatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final CatalogRepository catalogRepository;

    public ApiResponse<ApiData<ItemRepresentation>> register(CreateItemRequest request) {
        log.info("Registering new catalog item: name={}", request.getName());
        CatalogItem saved = catalogRepository.save(CatalogMapper.toEntity(request));
        log.info("Catalog item registered successfully: id={}", saved.getId());
        return CatalogMapper.toSingleResponse(saved);
    }

    public ApiResponse<ApiData<ItemRepresentation>> findById(Long id) {
        log.info("Fetching catalog item: id={}", id);
        CatalogItem item = catalogRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Catalog item not found: id={}", id);
                    return new ItemNotFoundException(id);
                });
        return CatalogMapper.toSingleResponse(item);
    }

    public ApiResponse<List<ApiData<ItemRepresentation>>> listAll() {
        log.info("Fetching all catalog items");
        List<CatalogItem> items = catalogRepository.findAll();
        log.info("Found {} catalog items", items.size());
        return CatalogMapper.toListResponse(items);
    }
}
