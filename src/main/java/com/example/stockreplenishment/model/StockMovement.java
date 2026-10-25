package com.example.stockreplenishment.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import static jakarta.persistence.EnumType.STRING;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_movements")
@EntityListeners(AuditingEntityListener.class)
public class StockMovement extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    private int quantity;

    @Column(length = 200)
    private String reason;

    @CreatedDate
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(length = 50)
    private String createdBy;

    protected StockMovement() {}

    public StockMovement(Product product, MovementType type, int quantity, String reason) {
        this.product = product;
        this.type = type;
        this.quantity = quantity;
        this.reason = reason;
    }

    public MovementType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}