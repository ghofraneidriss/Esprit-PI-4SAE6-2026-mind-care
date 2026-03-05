package tn.esprit.forums_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.forums_service.dto.CategoryDTO;
import tn.esprit.forums_service.entity.Category;
import tn.esprit.forums_service.service.CategoryService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200") // Allow Angular Frontend
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<Category> createCategory(@Valid @RequestBody Category category) {
        return new ResponseEntity<>(categoryService.createCategory(category), HttpStatus.CREATED);
    }

    @GetMapping
    public List<CategoryDTO> getAllCategories() {
        System.out.println(">>> Request: getAllCategories");
        List<Category> categories = categoryService.getAllCategories();
        System.out.println(">>> Found " + categories.size() + " categories");
        
        List<CategoryDTO> dtos = categories.stream()
            .map(cat -> CategoryDTO.builder()
                .id(cat.getId())
                .name(cat.getName())
                .description(cat.getDescription())
                .icon("ri-folder-line")
                .build())
            .collect(Collectors.toList());
            
        System.out.println(">>> Returning " + dtos.size() + " categories");
        return dtos;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @Valid @RequestBody Category categoryDetails) {
        return ResponseEntity.ok(categoryService.updateCategory(id, categoryDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
