package com.example.stockreplenishment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.stockreplenishment.dto.OrderLineRequest;
import com.example.stockreplenishment.dto.PurchaseOrderRequest;
import com.example.stockreplenishment.model.OrderStatus;
import com.example.stockreplenishment.model.PurchaseOrder;
import com.example.stockreplenishment.service.PurchaseOrderService;
import com.example.stockreplenishment.service.SupplierService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/purchase-orders")
public class PurchaseOrderController {
    private final PurchaseOrderService orderService;
    private final SupplierService supplierService;

    public PurchaseOrderController(PurchaseOrderService orderService, SupplierService supplierService) {
        this.orderService = orderService;
        this.supplierService = supplierService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("orders", orderService.findAll());
        return "purchase-orders/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("line", new OrderLineRequest(null, null, null));
        return showDetail(id, model);
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("order", new PurchaseOrderRequest(null));
        return showForm(model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("order") PurchaseOrderRequest request, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return showForm(model);
        }
        PurchaseOrder order = orderService.create(request);
        redirect.addFlashAttribute("success", "Purchase order " + order.getOrderNumber() + " created");
        return "redirect:/purchase-orders/" + order.getId();
    }

    @PostMapping("/{id}/items")
    public String addItem(@PathVariable Long id, @Valid @ModelAttribute("line") OrderLineRequest line,
                          BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return showDetail(id, model);
        }
        orderService.addItem(id, line);
        redirect.addFlashAttribute("success", "Item added");
        return "redirect:/purchase-orders/" + id;
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam OrderStatus status, RedirectAttributes redirect) {
        PurchaseOrder order = orderService.changeStatus(id, status);
        redirect.addFlashAttribute("success", "Order " + order.getOrderNumber() + " is now " + status);
        return "redirect:/purchase-orders/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        orderService.delete(id);
        redirect.addFlashAttribute("success", "Purchase order deleted");
        return "redirect:/purchase-orders";
    }

    // Turns a replenishment suggestion on the dashboard into an order line.
    @PostMapping("/replenish/{productId}")
    public String replenish(@PathVariable Long productId, RedirectAttributes redirect) {
        PurchaseOrder order = orderService.orderSuggestion(productId);
        redirect.addFlashAttribute("success", "Added to purchase order " + order.getOrderNumber()
                + " from " + order.getSupplier().getName());
        return "redirect:/";
    }

    private String showDetail(Long id, Model model) {
        model.addAttribute("order", orderService.findById(id));
        return "purchase-orders/detail";
    }

    private String showForm(Model model) {
        model.addAttribute("suppliers", supplierService.findActive());
        return "purchase-orders/form";
    }
}
