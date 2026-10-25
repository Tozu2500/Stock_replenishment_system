package com.example.stockreplenishment.model;

public enum StockStatus {

    OUT_OF_STOCK,
    LOW_STOCK,
    IN_STOCK;

    public static StockStatus of(int quantity, int minimumStock) {
        return quantity <= 0 ? OUT_OF_STOCK : quantity <= minimumStock ? LOW_STOCK : IN_STOCK;
    }

}
