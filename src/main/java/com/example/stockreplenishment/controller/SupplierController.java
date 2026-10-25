package com.example.stockreplenishment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.stockreplenishment.dto.SupplierRequest;
import com.example.stockreplenishment.model.Supplier;
import com.example.stockreplenishment.service.SupplierService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {
    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("suppliers", supplierService.findAll());
        return "suppliers/list";
    }

    @GetMapping({"/new", "/{id}/edit"})
    public String form(@PathVariable(required = false) Long id, Model model) {
        model.addAttribute("supplier", id == null ? new SupplierRequest(null, null, 7, true) : SupplierRequest.from(supplierService.findById(id)));
        model.addAttribute("supplierId", id);
        return "suppliers/form";
    }

    // POST /suppliers creates a supplier, POST /suppliers/{id} updates one.
    @PostMapping({"", "/{id}"})
    public String save(@PathVariable(required = false) Long id,
                       @Valid @ModelAttribute("supplier") SupplierRequest request, BindingResult result,
                       Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("supplierId", id);
            return "suppliers/form";
        }
        Supplier supplier = supplierService.save(id, request);
        redirect.addFlashAttribute("success", "Supplier " + supplier.getName() + " saved");
        return "redirect:/suppliers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        supplierService.delete(id);
        redirect.addFlashAttribute("success", "Supplier deleted");
        return "redirect:/suppliers";
    }
}
