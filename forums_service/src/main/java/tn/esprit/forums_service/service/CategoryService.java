package tn.esprit.forums_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.forums_service.dto.CategoryDTO;
import tn.esprit.forums_service.entity.Category;
import tn.esprit.forums_service.exception.ResourceNotFoundException;
import tn.esprit.forums_service.repository.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public Category createCategory(Category category) {
        if (category.getIcon() == null || category.getIcon().isBlank()) {
            category.setIcon("ri-folder-line");
        }
        if (category.getColor() == null || category.getColor().isBlank()) {
            category.setColor("#6366f1");
        }
        return categoryRepository.save(category);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /** Lightweight DTO list for APIs (no posts collection loaded). */
    public List<CategoryDTO> getAllCategoryDtos() {
        return getAllCategories().stream()
                .map(cat -> CategoryDTO.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .description(cat.getDescription())
                        .icon(cat.getIcon() != null && !cat.getIcon().isBlank() ? cat.getIcon() : "ri-folder-line")
                        .color(cat.getColor() != null && !cat.getColor().isBlank() ? cat.getColor() : "#6366f1")
                        .build())
                .collect(Collectors.toList());
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = getCategoryById(id);
        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        if (categoryDetails.getIcon() != null) {
            category.setIcon(categoryDetails.getIcon().isBlank() ? "ri-folder-line" : categoryDetails.getIcon());
        }
        if (categoryDetails.getColor() != null) {
            category.setColor(categoryDetails.getColor().isBlank() ? "#6366f1" : categoryDetails.getColor());
        }
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }
}
