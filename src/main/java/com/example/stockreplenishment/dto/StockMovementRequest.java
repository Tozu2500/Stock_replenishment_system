package com.example.stockreplenishment.dto;

import com.example.stockreplenishment.model.MovementType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// A manual stock change: a sale, a customer return, a receipt without an order or an adjustment (+/-).
public record StockMovementRequest(@NotNull MovementType type,
                                   @NotNull @Min(-1_000_000) @Max(1_000_000) Integer quantity,
                                   @Size(max = 200) String reason) {
}
