package com.example.stockreplenishment.model;

import java.util.Arrays;
import java.util.List;

public enum OrderStatus {

    PENDING,
    ORDERED,
    RECEIVED,
    CANCELLED;

    // Orders that are not finished yet: their quantities count as being an order
    public static final List<OrderStatus> OPEN = List.of(PENDING, ORDERED);

    public boolean canChangeTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == ORDERED || next == CANCELLED;
            case ORDERED -> next == RECEIVED || next == CANCELLED;
            case RECEIVED, CANCELLED -> false;
        };
    }

    public List<OrderStatus> getNextStatuses() {
        return Arrays.stream(values()).filter(this::canChangeTo).toList();
    }

    public boolean isEditable() {
        return this == PENDING;
    }

    public boolean isDeletable() {
        return this == PENDING || this == CANCELLED;
    }
}
