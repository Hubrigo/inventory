package com.hugo.inventory.controller;

import com.hugo.inventory.dto.*;
import com.hugo.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponse> create(@Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.create(request));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> findByProductId(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.findByProductId(productId));
    }

    @PostMapping("/purchase")
    public ResponseEntity<PurchaseResponse> purchase(
            @Valid @RequestBody PurchaseRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ResponseEntity.ok(inventoryService.purchase(request, idempotencyKey));
    }
}