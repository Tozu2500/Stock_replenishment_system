package com.example.stockreplenishment.controller;

import java.net.URI;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.stockreplenishment.exception.InvalidOperationException;

import jakarta.servlet.http.HttpServletRequest;

// Shared behaviour for all web (Thymeleaf) controllers.
@ControllerAdvice(basePackages = "com.example.stockreplenishment.controller")
public class WebControllerAdvice {

    // Trims form input and turns empty text fields into null.
    @InitBinder
    public void trimStrings(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    // A business rule stopped the operation: go back to the previous page and show the reason there.
    @ExceptionHandler(InvalidOperationException.class)
    public String handleRejectedOperation(RuntimeException ex, HttpServletRequest request, RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = request.getHeader("Referer");
        try {
            // Only the raw path of the previous page is used, so the redirect stays inside this application.
            String path = referer == null ? null : URI.create(referer).getRawPath();
            boolean local = path != null && path.startsWith("/") && !path.startsWith("//");
            return "redirect:" + (local ? path : "/");
        } catch (IllegalArgumentException e) {
            return "redirect:/";
        }
    }
}
