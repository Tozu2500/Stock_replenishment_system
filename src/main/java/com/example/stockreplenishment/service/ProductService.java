package com.example.stockreplenishment.service;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stockreplenishment.dto.ProductRequest;
import com.example.stockreplenishment.dto.StockMovementRequest;
import com.example.stockreplenishment.exception.InvalidOperationException;
import com.example.stockreplenishment.exception.ResourceNotFoundException;
import com.example.stockreplenishment.model.MovementType;
import com.example.stockreplenishment.model.Product;
import com.example.stockreplenishment.model.StockMovement;
import com.example.stockreplenishment.repository.ProductRepository;
import com.example.stockreplenishment.repository.PurchaseOrderRepository;
import com.example.stockreplenishment.repository.SupplierRepository;

@Service
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository orderRepository;

    public ProductService(ProductRepository productRepository, CategoryService categoryService,
                          SupplierRepository supplierRepository, PurchaseOrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.supplierRepository = supplierRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll(Long categoryId) {
        return categoryId == null ? productRepository.findAll(Sort.by("name"))
                : productRepository.findByCategoryIdOrderByName(categoryId);
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    // Creates a product when id is null, otherwise updates the existing one. SKUs must be unique.
    public Product save(Long id, ProductRequest request) {
        Product product = id == null ? new Product() : findById(id);
        String sku = request.sku().toUpperCase(Locale.ROOT); // ROOT: the result must not depend on the server language
        productRepository.findBySku(sku).filter(same -> !same.getId().equals(id)).ifPresent(same -> {
            throw new InvalidOperationException("SKU " + sku + " is already used by " + same.getName());
        });
        product.setName(request.name());
        product.setSku(sku);
        product.setUnitPrice(request.unitPrice());
        product.setMinimumStock(request.minimumStock());
        product.setTargetStock(request.targetStock());
        product.setActive(request.active());
        product.setCategory(categoryService.findById(request.categoryId()));
        product.getSuppliers().clear();
        for (Long supplierId : request.supplierIds()) { // findAllById would silently skip unknown ids
            product.getSuppliers().add(supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId)));
        }
        return productRepository.save(product);
    }

    public void delete(Long id) {
        Product product = findById(id);
        if (product.getQuantity() > 0) {
            throw new InvalidOperationException(product.getName() + " still has " + product.getQuantity()
                    + " units in stock and cannot be deleted");
        }
        if (orderRepository.existsByItemsProductId(id)) {
            throw new InvalidOperationException(product.getName()
                    + " appears on purchase orders. Deactivate it instead of deleting it.");
        }
        product.getCategory().getProducts().remove(product);
        productRepository.delete(product);
    }

    public Product recordMovement(Long id, StockMovementRequest request) {
        Product product = findById(id);
        changeStock(product, request.type(), request.type().toChange(request.quantity()), request.reason());
        return product;
    }

    // Every quantity change goes through here, so stock never goes negative and every change is logged.
    public void changeStock(Product product, MovementType type, int change, String reason) {
        if (change == 0) {
            throw new InvalidOperationException("Quantity change cannot be zero");
        }
        if (product.getQuantity() + change < 0) {
            throw new InvalidOperationException("Not enough stock: only " + product.getQuantity() + " units of "
                    + product.getName());
        }
        product.setQuantity(product.getQuantity() + change);
        product.getMovements().add(0, new StockMovement(product, type, change, reason));
    }
}
