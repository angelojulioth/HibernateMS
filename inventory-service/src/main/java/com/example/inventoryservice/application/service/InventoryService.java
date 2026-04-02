package com.example.inventoryservice.application.service;

import com.example.inventoryservice.domain.entity.InventoryItem;
import com.example.inventoryservice.infrastructure.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public Optional<InventoryItem> getBySku(String sku) {
        log.debug("Looking up inventory for SKU: {}", sku);
        return inventoryRepository.findBySku(sku);
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> getAll() {
        return inventoryRepository.findAll();
    }

    @Transactional
    public InventoryItem create(InventoryItem item) {
        log.info("Creating inventory item with SKU: {}", item.getSku());
        return inventoryRepository.save(item);
    }

    @Transactional
    public Optional<InventoryItem> updateQuantity(String sku, Integer quantity) {
        log.info("Updating quantity for SKU: {} to {}", sku, quantity);
        return inventoryRepository.findBySku(sku).map(item -> {
            item.setQuantity(quantity);
            item.setAvailable(quantity > 0);
            return inventoryRepository.save(item);
        });
    }

    @Transactional
    public boolean deleteBySku(String sku) {
        log.info("Deleting inventory item with SKU: {}", sku);
        return inventoryRepository.findBySku(sku).map(item -> {
            inventoryRepository.delete(item);
            return true;
        }).orElse(false);
    }
}
