package com.example.stockreplenishment.model;

import java.util.*;

import jakarta.persistence.*;

@Entity
@Table(name = "suppliers")
public class Supplier extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String email;

    private int leadTimeDays;
    private boolean active = true;

    @ManyToMany(mappedBy = "suppliers")
    @OrderBy("name")
    private Set<Product> products = new LinkedHashSet<>();

    public Supplier() {}

    public Supplier(String name, String email, int leadTimeDays) {
        this.name = name;
        this.email = email;
        this.leadTimeDays = leadTimeDays;
    }

    public String getName() { 
        return name; 
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(int leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<Product> getProducts() {
        return products;
    }
}
