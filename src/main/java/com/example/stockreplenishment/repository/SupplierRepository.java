package com.example.stockreplenishment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.stockreplenishment.model.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
}
