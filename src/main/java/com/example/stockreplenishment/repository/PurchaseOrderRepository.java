package com.example.stockreplenishment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.stockreplenishment.model.OrderStatus;
import com.example.stockreplenishment.model.PurchaseOrder;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findByStatusIn(Collection<OrderStatus> statuses);

    Optional<PurchaseOrder> findFirstBySupplierIdAndStatus(Long supplierId, OrderStatus status);

    boolean existsBySupplierId(Long supplierId);

    // True when the product is on a line of any order (path = items.product.id)
    boolean existsByItemsProductId(Long productId);

}
