package com.hugo.inventory.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseResponse {

    private UUID productId;
    private Integer purchasedQuantity;
    private Integer remainingStock;
    private String message;
}