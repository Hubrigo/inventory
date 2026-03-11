package com.hugo.inventory.service;

import com.hugo.inventory.dto.*;
import com.hugo.inventory.mapper.InventoryMapper;
import com.hugo.inventory.model.Inventory;
import com.hugo.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    public InventoryResponse create(InventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.getProductId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inventory already exists for this product"
            );
        }

        Inventory inventory = inventoryMapper.toEntity(request);
        Inventory saved = inventoryRepository.save(inventory);
        return inventoryMapper.toResponse(saved);
    }

    public InventoryResponse findByProductId(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Inventory not found for this product"
                ));

        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public PurchaseResponse purchase(PurchaseRequest request) {
        boolean inventoryExists = inventoryRepository.existsByProductId(request.getProductId());

        if (!inventoryExists) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Inventory not found for this product"
            );
        }

        int updatedRows = inventoryRepository.decreaseStockIfAvailable(
                request.getProductId(),
                request.getQuantity()
        );

        if (updatedRows == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient stock"
            );
        }

        Inventory updatedInventory = inventoryRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Inventory not found for this product"
                ));

        return PurchaseResponse.builder()
                .productId(updatedInventory.getProductId())
                .purchasedQuantity(request.getQuantity())
                .remainingStock(updatedInventory.getAvailable())
                .message("Purchase completed successfully")
                .build();
    }
}