package com.example.stockreplenishment.model;

public enum MovementType {

    RECEIPT,
    SALE,
    RETURN,
    ADJUSTMENT;

    // Turns the quantity a user typed into a signed stock change: sales remove stock
    public int toChange(int quantity) {
        return switch (this) {
            case RECEIPT, RETURN -> Math.abs(quantity);
            case SALE -> -Math.abs(quantity);
            case ADJUSTMENT -> quantity;
        };
    }

}
