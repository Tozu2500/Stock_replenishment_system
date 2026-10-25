package com.example.stockreplenishment.dto;

import com.example.stockreplenishment.model.Category;
import jakarta.validation.constraints.*;

public record CategoryRequest(@NotBlank @Size(max = 100) String name) {
    public static CategoryRequest from(Category category) {
        return new CategoryRequest(category.getName());
    }
}
