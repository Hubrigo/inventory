package com.hugo.inventory.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {

    private UUID id;
    private UUID productId;
    private Integer available;
    private Instant createdAt;
}