package com.example.stockreplenishment.dto;

import com.example.stockreplenishment.model.*;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(Long id, String name, String sku, BigDecimal unitPrice,
                              int quantity, int minimumStock, int targetStock, StockStatus status, boolean active,
                              Long categoryId, String categoryName, List<Long> supplierIds) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getSku(), p.getUnitPrice(),
                p.getQuantity(), p.getMinimumStock(), p.getTargetStock(), p.getStatus(), p.isActive(),
                p.getCategory().getId(), p.getCategory().getName(),
                p.getSuppliers().stream().map(Supplier::getId).toList());
    }
}
