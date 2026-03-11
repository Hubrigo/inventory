package com.hugo.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hugo.inventory.client.ProductClient;
import com.hugo.inventory.dto.*;
import com.hugo.inventory.mapper.InventoryMapper;
import com.hugo.inventory.model.IdempotencyRecord;
import com.hugo.inventory.model.Inventory;
import com.hugo.inventory.repository.IdempotencyRecordRepository;
import com.hugo.inventory.repository.InventoryRepository;
import com.hugo.inventory.util.IdempotencyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;
    private final ProductClient productClient;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final IdempotencyUtil idempotencyUtil;
    private final ObjectMapper objectMapper;

    public InventoryResponse create(InventoryRequest request) {

        log.info("Creating inventory for productId={}, available={}",
                request.getProductId(), request.getAvailable());
        productClient.validateProductExists(request.getProductId()).join();

        if (inventoryRepository.existsByProductId(request.getProductId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inventory already exists for this product"
            );
        }

        Inventory inventory = inventoryMapper.toEntity(request);
        Inventory saved = inventoryRepository.save(inventory);

        log.info("Inventory created successfully for productId={}", request.getProductId());

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
    public PurchaseResponse purchase(PurchaseRequest request, String idempotencyKey) {
        log.info("Processing purchase for productId={}, quantity={}",
                request.getProductId(), request.getQuantity());
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key header is required"
            );
        }

        String requestHash = idempotencyUtil.buildRequestHash(request);

        IdempotencyRecord existingRecord = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (existingRecord != null) {
            if (!existingRecord.getRequestHash().equals(requestHash)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency-Key already used with different request"
                );
            }

            try {
                return objectMapper.readValue(existingRecord.getResponseBody(), PurchaseResponse.class);
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to read stored idempotent response"
                );
            }
        }

        productClient.validateProductExists(request.getProductId()).join();

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

            log.warn("Insufficient stock for productId={}, requestedQuantity={}",
                    request.getProductId(), request.getQuantity());

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

        PurchaseResponse response = PurchaseResponse.builder()
                .productId(updatedInventory.getProductId())
                .purchasedQuantity(request.getQuantity())
                .remainingStock(updatedInventory.getAvailable())
                .message("Purchase completed successfully")
                .build();

        try {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .requestHash(requestHash)
                    .responseBody(objectMapper.writeValueAsString(response))
                    .responseStatus(HttpStatus.OK.value())
                    .build();

            idempotencyRecordRepository.save(record);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store idempotent response"
            );
        }

        log.info("Purchase completed for productId={}, purchasedQuantity={}, remainingStock={}",
                updatedInventory.getProductId(),
                request.getQuantity(),
                updatedInventory.getAvailable());

        return response;
    }
}