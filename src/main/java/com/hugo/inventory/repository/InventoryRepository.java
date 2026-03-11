package com.hugo.inventory.repository;

import com.hugo.inventory.model.Inventory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductId(UUID productId);

    boolean existsByProductId(UUID productId);

    @Modifying
    @Query("""
        update Inventory i
           set i.available = i.available - :quantity
         where i.productId = :productId
           and i.available >= :quantity
    """)
    int decreaseStockIfAvailable(@Param("productId") UUID productId,
                                 @Param("quantity") Integer quantity);
}