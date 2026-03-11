package com.hugo.inventory.util;

import com.hugo.inventory.dto.PurchaseRequest;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyUtil {

    public String buildRequestHash(PurchaseRequest request) {
        return request.getProductId() + ":" + request.getQuantity();
    }
}