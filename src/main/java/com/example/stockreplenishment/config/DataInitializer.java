package com.example.stockreplenishment.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.stockreplenishment.dto.OrderLineRequest;
import com.example.stockreplenishment.dto.PurchaseOrderRequest;
import com.example.stockreplenishment.model.AppUser;
import com.example.stockreplenishment.model.Category;
import com.example.stockreplenishment.model.MovementType;
import com.example.stockreplenishment.model.OrderStatus;
import com.example.stockreplenishment.model.Product;
import com.example.stockreplenishment.model.PurchaseOrder;
import com.example.stockreplenishment.model.Role;
import com.example.stockreplenishment.model.Supplier;
import com.example.stockreplenishment.repository.AppUserRepository;
import com.example.stockreplenishment.repository.CategoryRepository;
import com.example.stockreplenishment.repository.ProductRepository;
import com.example.stockreplenishment.repository.SupplierRepository;
import com.example.stockreplenishment.service.ProductService;
import com.example.stockreplenishment.service.PurchaseOrderService;

// Fills an empty database with demo users and inventory data when the application starts.
@Component
public class DataInitializer implements CommandLineRunner {
    private final AppUserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final PurchaseOrderService orderService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}") private boolean enabled;
    @Value("${app.seed.admin-password}") private String adminPassword;
    @Value("${app.seed.user-password}") private String userPassword;

    public DataInitializer(AppUserRepository userRepository, CategoryRepository categoryRepository,
                           SupplierRepository supplierRepository, ProductRepository productRepository,
                           ProductService productService, PurchaseOrderService orderService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.orderService = orderService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || userRepository.count() > 0) {
            return;
        }
        userRepository.save(new AppUser("admin", passwordEncoder.encode(adminPassword), Role.ADMIN));
        userRepository.save(new AppUser("user", passwordEncoder.encode(userPassword), Role.USER));
        Category electronics = categoryRepository.save(new Category("Electronics"));
        Category office = categoryRepository.save(new Category("Office Supplies"));
        Supplier nordic = supplierRepository.save(new Supplier("Nordic Components Oy", "sales@nordic.example", 5));
        Supplier techParts = supplierRepository.save(new Supplier("TechParts Europe GmbH", "orders@techparts.example", 10));
        Supplier paperworks = supplierRepository.save(new Supplier("Paperworks Finland Oy", "sales@paperworks.example", 3));

        product("Mechanical Keyboard", "KB-100", "49.90", 10, 50, 27, electronics, nordic, techParts);
        product("USB-C Cable 2 m", "USB-C-2M", "9.90", 20, 50, 8, electronics, nordic, techParts);
        product("Wireless Mouse", "MS-200", "24.90", 15, 40, 0, electronics, techParts);
        Product monitor = product("27-inch Monitor", "MON-27", "229.00", 5, 15, 4, electronics, techParts);
        product("Copy Paper A4, 500 sheets", "PAPER-A4", "5.49", 30, 100, 148, office, paperworks);
        product("Ballpoint Pens, 10 pcs", "PEN-10", "3.90", 20, 80, 64, office, paperworks);

        // An order already sent to the supplier: its monitors count as being on order.
        PurchaseOrder order = orderService.create(new PurchaseOrderRequest(techParts.getId()));
        orderService.addItem(order.getId(), new OrderLineRequest(monitor.getId(), 11, null));
        orderService.changeStatus(order.getId(), OrderStatus.ORDERED);
    }

    private Product product(String name, String sku, String price, int minimum, int target, int quantity,
                            Category category, Supplier... suppliers) {
        Product product = new Product(name, sku, new BigDecimal(price), minimum, target, category);
        product.getSuppliers().addAll(List.of(suppliers));
        productRepository.save(product);
        if (quantity > 0) {
            productService.changeStock(product, MovementType.ADJUSTMENT, quantity, "Initial stock");
        }
        return product;
    }
}
