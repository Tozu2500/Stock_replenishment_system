package com.example.stockreplenishment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

// One purchase order line. Without a unit price the list price of the product is used.
public record OrderLineRequest(@NotNull(message = "choose a product") Long productId,
                               @NotNull @Positive @Max(1_000_000) Integer quantity,
                               @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal unitPrice) {
}
