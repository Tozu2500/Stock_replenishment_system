package com.example.stockreplenishment.model;

public enum MovementType {

    RECEIPT,
    SALE,
    RETURN,
    ADJUSTMENT;

    public int toChange(int quantity) {
        return switch (this) {
            case RECEIPT, RETURN -> Math.abs(quantity);
            case SALE -> -Math.abs(quantity);
            case ADJUSTMENT -> quantity;
        };
    }

}
