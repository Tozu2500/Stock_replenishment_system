package com.example.stockreplenishment.service;

import com.example.stockreplenishment.dto.*;
import com.example.stockreplenishment.exception.InvalidOperationException;
import com.example.stockreplenishment.model.*;
import com.example.stockreplenishment.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/** Integration tests of stock, purchase order and replenishment logic against an H2 database. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InventoryServiceTest {
    @Autowired private PurchaseOrderService orderService;
    @Autowired private ProductService productService;
    @Autowired private ReplenishmentService replenishmentService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private SupplierRepository supplierRepository;

    private Product keyboard;
    private Supplier supplier;

    /** A keyboard with 5 units in stock, minimum 10 and target 50, supplied with a lead time of 5 days. */
    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(new Category("Electronics"));
        supplier = supplierRepository.save(new Supplier("Nordic Components", "sales@nordic.example", 5));
        keyboard = new Product("Keyboard", "KB-1", new BigDecimal("49.90"), 10, 50, category);
        keyboard.getSuppliers().add(supplier);
        productRepository.save(keyboard);
        productService.changeStock(keyboard, MovementType.ADJUSTMENT, 5, "Initial stock");
    }

    @Test
    void saleReducesStockButCannotMakeItNegative() {
        productService.recordMovement(keyboard.getId(), new StockMovementRequest(MovementType.SALE, 3, "Webshop"));
        assertThat(keyboard.getQuantity()).isEqualTo(2);

        assertThatThrownBy(() -> productService.recordMovement(keyboard.getId(),
                new StockMovementRequest(MovementType.SALE, 3, null)))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Not enough stock");
    }

    @Test
    void newOrderIsPendingWithNumberDeliveryDateAndTotal() {
        PurchaseOrder order = orderOf(45);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getOrderNumber()).startsWith("PO-");
        assertThat(order.getExpectedDeliveryDate()).isEqualTo(LocalDate.now().plusDays(5));
        assertThat(order.getTotal()).isEqualByComparingTo("2245.50");
        assertThatThrownBy(() -> orderService.changeStatus(order.getId(), OrderStatus.RECEIVED))
                .hasMessageContaining("cannot change from PENDING to RECEIVED");
    }

    @Test
    void receivingOrderAddsStockAndRecordsMovement() {
        PurchaseOrder order = orderOf(45);
        orderService.changeStatus(order.getId(), OrderStatus.ORDERED);
        orderService.changeStatus(order.getId(), OrderStatus.RECEIVED);

        assertThat(keyboard.getQuantity()).isEqualTo(50);
        assertThat(keyboard.getStatus()).isEqualTo(StockStatus.IN_STOCK);
        assertThat(keyboard.getMovements().getFirst().getType()).isEqualTo(MovementType.RECEIPT);
        assertThatThrownBy(() -> orderService.addItem(order.getId(), new OrderLineRequest(keyboard.getId(), 1, null)))
                .hasMessageContaining("can no longer be changed");
    }

    @Test
    void ordersRequireActiveSupplierThatSuppliesTheProduct() {
        Supplier other = supplierRepository.save(new Supplier("Other Ltd", "info@other.example", 3));
        assertThatThrownBy(() -> orderService.changeStatus(orderService.create(new PurchaseOrderRequest(other.getId()))
                .getId(), OrderStatus.ORDERED)).hasMessageContaining("at least one item");
        PurchaseOrder order = orderService.create(new PurchaseOrderRequest(other.getId()));
        assertThatThrownBy(() -> orderService.addItem(order.getId(), new OrderLineRequest(keyboard.getId(), 1, null)))
                .hasMessageContaining("does not supply");

        other.setActive(false);
        assertThatThrownBy(() -> orderService.create(new PurchaseOrderRequest(other.getId())))
                .hasMessageContaining("inactive");
    }

    @Test
    void replenishmentSuggestionBecomesOrderLineAndDisappears() {
        assertThat(replenishmentService.findSuggestions()).singleElement()
                .satisfies(suggestion -> assertThat(suggestion.recommendedQuantity()).isEqualTo(45));

        PurchaseOrder order = orderService.orderSuggestion(keyboard.getId());

        assertThat(order.getItems()).singleElement().satisfies(item -> assertThat(item.getQuantity()).isEqualTo(45));
        assertThat(replenishmentService.findSuggestions()).isEmpty();
    }

    private PurchaseOrder orderOf(int quantity) {
        PurchaseOrder order = orderService.create(new PurchaseOrderRequest(supplier.getId()));
        return orderService.addItem(order.getId(), new OrderLineRequest(keyboard.getId(), quantity, null));
    }
}
