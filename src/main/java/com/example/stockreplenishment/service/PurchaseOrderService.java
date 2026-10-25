package com.example.stockreplenishment.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stockreplenishment.dto.OrderLineRequest;
import com.example.stockreplenishment.dto.PurchaseOrderRequest;
import com.example.stockreplenishment.dto.ReplenishmentSuggestion;
import com.example.stockreplenishment.exception.InvalidOperationException;
import com.example.stockreplenishment.exception.ResourceNotFoundException;
import com.example.stockreplenishment.model.MovementType;
import com.example.stockreplenishment.model.OrderStatus;
import com.example.stockreplenishment.model.Product;
import com.example.stockreplenishment.model.PurchaseOrder;
import com.example.stockreplenishment.model.PurchaseOrderItem;
import com.example.stockreplenishment.model.Supplier;
import com.example.stockreplenishment.repository.PurchaseOrderRepository;

@Service
@Transactional
public class PurchaseOrderService {
    private final PurchaseOrderRepository orderRepository;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final ReplenishmentService replenishmentService;

    public PurchaseOrderService(PurchaseOrderRepository orderRepository, SupplierService supplierService,
                                ProductService productService, ReplenishmentService replenishmentService) {
        this.orderRepository = orderRepository;
        this.supplierService = supplierService;
        this.productService = productService;
        this.replenishmentService = replenishmentService;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> findAll() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate", "id"));
    }

    @Transactional(readOnly = true)
    public PurchaseOrder findById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Purchase order", id));
    }

    // Creates an empty order that is expected to arrive after the lead time of the supplier.
    public PurchaseOrder create(PurchaseOrderRequest request) {
        Supplier supplier = supplierService.findById(request.supplierId());
        if (!supplier.isActive()) {
            throw new InvalidOperationException("Supplier " + supplier.getName() + " is inactive and cannot receive orders");
        }
        PurchaseOrder order = new PurchaseOrder();
        order.setSupplier(supplier);
        order.setExpectedDeliveryDate(LocalDate.now().plusDays(supplier.getLeadTimeDays()));
        PurchaseOrder saved = orderRepository.save(order);
        saved.setOrderNumber(String.format("PO-%d-%04d", saved.getOrderDate().getYear(), saved.getId()));
        return saved;
    }

    public PurchaseOrder addItem(Long orderId, OrderLineRequest line) {
        PurchaseOrder order = findById(orderId);
        if (!order.getStatus().isEditable()) {
            throw new InvalidOperationException("Order " + order.getOrderNumber() + " is " + order.getStatus()
                    + " and can no longer be changed");
        }
        Product product = productService.findById(line.productId());
        if (!product.isActive()) {
            throw new InvalidOperationException(product.getName() + " is inactive and cannot be ordered");
        }
        // Uses the many-to-many relationship: a supplier can only be sent orders for products it supplies.
        if (!product.getSuppliers().contains(order.getSupplier())) {
            throw new InvalidOperationException(order.getSupplier().getName() + " does not supply " + product.getName());
        }
        BigDecimal price = line.unitPrice() != null ? line.unitPrice() : product.getUnitPrice();
        order.addItem(product, line.quantity(), price);
        return order;
    }

    // PENDING -> ORDERED -> RECEIVED, or CANCELLED. Receiving adds every line to stock in one transaction.
    public PurchaseOrder changeStatus(Long id, OrderStatus newStatus) {
        PurchaseOrder order = findById(id);
        if (!order.getStatus().canChangeTo(newStatus)) {
            throw new InvalidOperationException("Order " + order.getOrderNumber() + " cannot change from "
                    + order.getStatus() + " to " + newStatus);
        }
        if (newStatus == OrderStatus.ORDERED && order.getItems().isEmpty()) {
            throw new InvalidOperationException("Add at least one item before sending the order");
        }
        if (newStatus == OrderStatus.RECEIVED) {
            for (PurchaseOrderItem item : order.getItems()) {
                productService.changeStock(item.getProduct(), MovementType.RECEIPT, item.getQuantity(),
                        "Received " + order.getOrderNumber());
            }
        }
        order.setStatus(newStatus);
        return order;
    }

    public void delete(Long id) {
        PurchaseOrder order = findById(id);
        if (!order.getStatus().isDeletable()) {
            throw new InvalidOperationException("Only pending or cancelled orders can be deleted. "
                    + order.getOrderNumber() + " is " + order.getStatus() + ".");
        }
        orderRepository.delete(order);
    }

    // Adds a replenishment suggestion to a pending order of the preferred supplier, or to a new order.
    public PurchaseOrder orderSuggestion(Long productId) {
        ReplenishmentSuggestion suggestion = replenishmentService.suggestionFor(productService.findById(productId));
        String name = suggestion.product().getName();
        if (suggestion.recommendedQuantity() == 0) {
            throw new InvalidOperationException("Stock of " + name + " is sufficient, nothing to order");
        }
        if (suggestion.supplier() == null) {
            throw new InvalidOperationException(name + " has no active supplier");
        }
        Long supplierId = suggestion.supplier().getId();
        PurchaseOrder order = orderRepository.findFirstBySupplierIdAndStatus(supplierId, OrderStatus.PENDING)
                .orElseGet(() -> create(new PurchaseOrderRequest(supplierId)));
        return addItem(order.getId(), new OrderLineRequest(productId, suggestion.recommendedQuantity(), null));
    }
}
