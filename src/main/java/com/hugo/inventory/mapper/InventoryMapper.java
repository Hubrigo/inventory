package com.hugo.inventory.mapper;

import com.hugo.inventory.dto.InventoryRequest;
import com.hugo.inventory.dto.InventoryResponse;
import com.hugo.inventory.model.Inventory;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public Inventory toEntity(InventoryRequest request) {
        return Inventory.builder()
                .productId(request.getProductId())
                .available(request.getAvailable())
                .build();
    }

    public InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .available(inventory.getAvailable())
                .createdAt(inventory.getCreatedAt())
                .build();
    }
}