package com.example.stockreplenishment.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stockreplenishment.dto.ReplenishmentSuggestion;
import com.example.stockreplenishment.model.OrderStatus;
import com.example.stockreplenishment.model.Product;
import com.example.stockreplenishment.model.PurchaseOrderItem;
import com.example.stockreplenishment.model.Supplier;
import com.example.stockreplenishment.repository.ProductRepository;
import com.example.stockreplenishment.repository.PurchaseOrderRepository;

@Service
@Transactional(readOnly = true)
public class ReplenishmentService {
    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public ReplenishmentService(ProductRepository productRepository, PurchaseOrderRepository purchaseOrderRepository) {
        this.productRepository = productRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    // The reorder rule: when on hand + on order has fallen to the minimum, order up to the target level.
    public static int recommendedQuantity(int onHand, int onOrder, int minimumStock, int targetStock) {
        int position = onHand + onOrder;
        return position > minimumStock ? 0 : Math.max(targetStock - position, 0);
    }

    // Active products that should be reordered, out-of-stock products first.
    public List<ReplenishmentSuggestion> findSuggestions() {
        Map<Long, Integer> onOrder = quantitiesOnOrder();
        return productRepository.findAll().stream()
                .filter(Product::isActive)
                .map(product -> toSuggestion(product, onOrder))
                .filter(suggestion -> suggestion.recommendedQuantity() > 0)
                .sorted(Comparator.comparing((ReplenishmentSuggestion s) -> s.product().getStatus())
                        .thenComparing(s -> s.product().getName()))
                .toList();
    }

    public ReplenishmentSuggestion suggestionFor(Product product) {
        return toSuggestion(product, quantitiesOnOrder());
    }

    private ReplenishmentSuggestion toSuggestion(Product product, Map<Long, Integer> onOrder) {
        int ordered = onOrder.getOrDefault(product.getId(), 0);
        int recommended = recommendedQuantity(product.getQuantity(), ordered, product.getMinimumStock(),
                product.getTargetStock());
        // Preferred supplier: the active supplier that delivers fastest.
        Supplier supplier = product.getSuppliers().stream().filter(Supplier::isActive)
                .min(Comparator.comparingInt(Supplier::getLeadTimeDays)).orElse(null);
        return new ReplenishmentSuggestion(product, ordered, recommended, supplier);
    }

    // Quantities on open purchase orders, summed per product.
    private Map<Long, Integer> quantitiesOnOrder() {
        return purchaseOrderRepository.findByStatusIn(OrderStatus.OPEN).stream().flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(i -> i.getProduct().getId(), Collectors.summingInt(PurchaseOrderItem::getQuantity)));
    }
}
