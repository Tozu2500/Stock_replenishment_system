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

import com.example.stockreplenishment.dto.ProductRequest;
import com.example.stockreplenishment.dto.StockMovementRequest;
import com.example.stockreplenishment.model.MovementType;
import com.example.stockreplenishment.model.Product;
import com.example.stockreplenishment.service.CategoryService;
import com.example.stockreplenishment.service.ProductService;
import com.example.stockreplenishment.service.SupplierService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final SupplierService supplierService;

    public ProductController(ProductService productService, CategoryService categoryService,
                             SupplierService supplierService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.supplierService = supplierService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long categoryId, Model model) {
        model.addAttribute("products", productService.findAll(categoryId));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("categoryId", categoryId);
        return "products/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("movement", new StockMovementRequest(MovementType.SALE, null, null));
        return showDetail(id, model);
    }

    // Records a sale, return, receipt or adjustment for the product.
    @PostMapping("/{id}/movements")
    public String recordMovement(@PathVariable Long id, @Valid @ModelAttribute("movement") StockMovementRequest request,
                                 BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return showDetail(id, model);
        }
        Product product = productService.recordMovement(id, request);
        redirect.addFlashAttribute("success", "Stock updated, " + product.getQuantity() + " units now");
        return "redirect:/products/" + id;
    }

    @GetMapping({"/new", "/{id}/edit"})
    public String form(@PathVariable(required = false) Long id, Model model) {
        model.addAttribute("product", id == null ? new ProductRequest(null, null, null, null, null, true, null, null)
                : ProductRequest.from(productService.findById(id)));
        return showForm(model, id);
    }

    // POST /products creates a product, POST /products/{id} updates one.
    @PostMapping({"", "/{id}"})
    public String save(@PathVariable(required = false) Long id,
                       @Valid @ModelAttribute("product") ProductRequest request, BindingResult result,
                       Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return showForm(model, id);
        }
        Product product = productService.save(id, request);
        redirect.addFlashAttribute("success", "Product " + product.getName() + " saved");
        return "redirect:/products/" + product.getId();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        productService.delete(id);
        redirect.addFlashAttribute("success", "Product deleted");
        return "redirect:/products";
    }

    private String showDetail(Long id, Model model) {
        model.addAttribute("product", productService.findById(id));
        model.addAttribute("movementTypes", MovementType.values());
        return "products/detail";
    }

    // The form object is already in the model; replacing it here would drop its validation errors.
    private String showForm(Model model, Long id) {
        model.addAttribute("productId", id);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("suppliers", supplierService.findAll());
        return "products/form";
    }
}
