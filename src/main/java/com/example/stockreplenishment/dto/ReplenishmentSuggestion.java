package com.example.stockreplenishment.dto;

import com.example.stockreplenishment.model.Product;
import com.example.stockreplenishment.model.Supplier;

// A product that should be reordered, with the recommended quantity and the preferred supplier (may be null).
public record ReplenishmentSuggestion(Product product, int onOrder, int recommendedQuantity, Supplier supplier) {
}
