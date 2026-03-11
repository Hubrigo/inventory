package com.hugo.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryRequest {

    @NotNull(message = "Product id is required")
    private UUID productId;

    @NotNull(message = "Available stock is required")
    @Min(value = 0, message = "Available stock must be 0 or greater")
    private Integer available;
}