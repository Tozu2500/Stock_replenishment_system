package com.example.stockreplenishment.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stockreplenishment.dto.SupplierRequest;
import com.example.stockreplenishment.exception.InvalidOperationException;
import com.example.stockreplenishment.exception.ResourceNotFoundException;
import com.example.stockreplenishment.model.Supplier;
import com.example.stockreplenishment.repository.PurchaseOrderRepository;
import com.example.stockreplenishment.repository.SupplierRepository;

@Service
@Transactional
public class SupplierService {
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public SupplierService(SupplierRepository supplierRepository, PurchaseOrderRepository purchaseOrderRepository) {
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<Supplier> findAll() {
        return supplierRepository.findAll(Sort.by("name"));
    }

    @Transactional(readOnly = true)
    public List<Supplier> findActive() {
        return findAll().stream().filter(Supplier::isActive).toList();
    }

    @Transactional(readOnly = true)
    public Supplier findById(Long id) {
        return supplierRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
    }

    // Creates a supplier when id is null, otherwise updates the existing one.
    public Supplier save(Long id, SupplierRequest request) {
        Supplier supplier = id == null ? new Supplier() : findById(id);
        supplier.setName(request.name());
        supplier.setEmail(request.email());
        supplier.setLeadTimeDays(request.leadTimeDays());
        supplier.setActive(request.active());
        return supplierRepository.save(supplier);
    }

    public void delete(Long id) {
        Supplier supplier = findById(id);
        if (purchaseOrderRepository.existsBySupplierId(id)) {
            throw new InvalidOperationException("Supplier " + supplier.getName()
                    + " has purchase orders. Mark it inactive instead of deleting it.");
        }
        // Product owns the many-to-many relationship, so the links are removed from the product side.
        supplier.getProducts().forEach(product -> product.getSuppliers().remove(supplier));
        supplierRepository.delete(supplier);
    }
}
