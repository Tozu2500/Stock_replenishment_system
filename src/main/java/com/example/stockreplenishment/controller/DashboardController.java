package com.example.stockreplenishment.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.stockreplenishment.model.Product;
import com.example.stockreplenishment.model.StockStatus;
import com.example.stockreplenishment.service.ProductService;
import com.example.stockreplenishment.service.ReplenishmentService;

@Controller
public class DashboardController {
    private final ProductService productService;
    private final ReplenishmentService replenishmentService;

    public DashboardController(ProductService productService, ReplenishmentService replenishmentService) {
        this.productService = productService;
        this.replenishmentService = replenishmentService;
    }

    // Key figures and the replenishment suggestions, which an admin can order with one click.
    @GetMapping("/")
    public String dashboard(Model model) {
        List<Product> products = productService.findAll(null);
        model.addAttribute("productCount", products.size());
        model.addAttribute("lowStock", products.stream().filter(p -> p.getStatus() == StockStatus.LOW_STOCK).count());
        model.addAttribute("outOfStock", products.stream().filter(p -> p.getStatus() == StockStatus.OUT_OF_STOCK).count());
        model.addAttribute("stockValue", products.stream().map(Product::getStockValue).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("suggestions", replenishmentService.findSuggestions());
        return "dashboard";
    }
}
