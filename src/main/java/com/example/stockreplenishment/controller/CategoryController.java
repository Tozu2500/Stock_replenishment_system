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

import com.example.stockreplenishment.dto.CategoryRequest;
import com.example.stockreplenishment.service.CategoryService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "categories/list";
    }

    @GetMapping({"/new", "/{id}/edit"})
    public String form(@PathVariable(required = false) Long id, Model model) {
        model.addAttribute("category", id == null ? new CategoryRequest(null) : CategoryRequest.from(categoryService.findById(id)));
        model.addAttribute("categoryId", id);
        return "categories/form";
    }

    // POST /categories creates a category, POST /categories/{id} updates one.
    @PostMapping({"", "/{id}"})
    public String save(@PathVariable(required = false) Long id,
                       @Valid @ModelAttribute("category") CategoryRequest request, BindingResult result,
                       Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("categoryId", id);
            return "categories/form";
        }
        redirect.addFlashAttribute("success", "Category " + categoryService.save(id, request).getName() + " saved");
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        categoryService.delete(id);
        redirect.addFlashAttribute("success", "Category deleted");
        return "redirect:/categories";
    }
}
