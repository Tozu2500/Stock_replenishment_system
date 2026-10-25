package com.example.stockreplenishment.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stockreplenishment.dto.CategoryRequest;
import com.example.stockreplenishment.exception.InvalidOperationException;
import com.example.stockreplenishment.exception.ResourceNotFoundException;
import com.example.stockreplenishment.model.Category;
import com.example.stockreplenishment.repository.CategoryRepository;

@Service
@Transactional
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll(Sort.by("name"));
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    // Creates a category when id is null, otherwise updates the existing one. Names must be unique.
    public Category save(Long id, CategoryRequest request) {
        Category category = id == null ? new Category() : findById(id);
        categoryRepository.findByNameIgnoreCase(request.name())
                .filter(same -> !same.getId().equals(id))
                .ifPresent(same -> {
                    throw new InvalidOperationException("Category " + request.name() + " already exists");
                });
        category.setName(request.name());
        return categoryRepository.save(category);
    }

    public void delete(Long id) {
        Category category = findById(id);
        if (!category.getProducts().isEmpty()) {
            throw new InvalidOperationException("Category " + category.getName() + " still contains "
                    + category.getProducts().size() + " product(s) and cannot be deleted");
        }
        categoryRepository.delete(category);
    }
}
