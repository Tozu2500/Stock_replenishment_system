package com.example.stockreplenishment.dto;

import jakarta.validation.constraints.NotNull;

// Order header. Lines are added to the order one by one after it has been created.
public record PurchaseOrderRequest(@NotNull(message = "choose a supplier") Long supplierId) {
}
