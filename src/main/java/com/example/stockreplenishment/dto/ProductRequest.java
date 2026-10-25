package com.example.stockreplenishment.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.stockreplenishment.model.Product;
import com.example.stockreplenishment.model.Supplier;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 40)
        @Pattern(regexp = "[A-Za-z0-9-]*", message = "may only contain letters, digits and dashes") String sku,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal unitPrice,
        @NotNull @PositiveOrZero Integer minimumStock,
        @NotNull @PositiveOrZero Integer targetStock,
        Boolean active,
        @NotNull(message = "choose a category") Long categoryId,
        Set<Long> supplierIds) {

    public ProductRequest {
        active = active == null || active; // a product is active unless the request says otherwise
        supplierIds = supplierIds == null ? Set.of() : supplierIds;
    }

    // Cross-field rule: the target stock level cannot be below the minimum stock level.
    @AssertTrue(message = "target stock must be greater than or equal to minimum stock")
    public boolean isStockLevelsValid() {
        return minimumStock == null || targetStock == null || targetStock >= minimumStock;
    }

    public static ProductRequest from(Product product) {
        return new ProductRequest(product.getName(), product.getSku(),
                product.getUnitPrice(), product.getMinimumStock(), product.getTargetStock(), product.isActive(),
                product.getCategory().getId(),
                product.getSuppliers().stream().map(Supplier::getId).collect(Collectors.toSet()));
    }
}
