package com.example.stockreplenishment.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    // SKU = STOCK KEEPING UNIT!
    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    private int quantity;
    private int minimumStock;
    private int targetStock;
    private boolean active = true;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    // Product is the owning side of many-to-many relationships, so it writes
    @ManyToMany
    @JoinTable(name = "product_suppliers", joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "supplier_id"))
    @OrderBy("name")
    private Set<Supplier> suppliers = new LinkedHashSet<>();

    // Stock movements saved and deleted together with product
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @OrderBy("createdAt DESC, id DESC")
    private final List<StockMovement> movements = new ArrayList<>();

    public Product() {}

    public Product(String name, String sku, BigDecimal unitPrice, int minimumStock, int targetStock,
                    Category category) {
        this.name = name;
        this.sku = sku;
        this.unitPrice = unitPrice;
        this.minimumStock = minimumStock;
        this.targetStock = targetStock;
        this.category = category;
    }

    public StockStatus getStatus() {
        return StockStatus.of(quantity, minimumStock);
    }

    public BigDecimal getStockValue() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getMinimumStock() {
        return minimumStock;
    }

    public void setMinimumStock(int minimumStock) {
        this.minimumStock = minimumStock;
    }

    public int getTargetStock() {
        return targetStock;
    }

    public void setTargetStock(int targetStock) {
        this.targetStock = targetStock;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Set<Supplier> getSuppliers() {
        return suppliers;
    }

    public List<StockMovement> getMovements() {
        return movements;
    }
}
