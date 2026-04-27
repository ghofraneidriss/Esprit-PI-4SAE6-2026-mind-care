package tn.esprit.forums_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.forums_service.dto.CategoryDTO;
import tn.esprit.forums_service.entity.Category;
import tn.esprit.forums_service.exception.ResourceNotFoundException;
import tn.esprit.forums_service.repository.CategoryRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    public void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Health");
        category.setDescription("Health related topics");
        category.setIcon("ri-heart-line");
        category.setColor("#ff0000");
    }

    @Test
    public void testCreateCategory_withIconAndColor() {
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        Category result = categoryService.createCategory(category);
        assertNotNull(result);
        assertEquals("Health", result.getName());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    public void testCreateCategory_withoutIcon_setsDefault() {
        category.setIcon(null);
        category.setColor(null);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        Category result = categoryService.createCategory(category);
        assertNotNull(result);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    public void testGetAllCategories() {
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category));
        List<Category> result = categoryService.getAllCategories();
        assertEquals(1, result.size());
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    public void testGetAllCategoryDtos() {
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category));
        List<CategoryDTO> result = categoryService.getAllCategoryDtos();
        assertEquals(1, result.size());
        assertEquals("Health", result.get(0).getName());
    }

    @Test
    public void testGetCategoryById_found() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        Category result = categoryService.getCategoryById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    public void testGetCategoryById_notFound_throwsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryById(99L));
    }

    @Test
    public void testUpdateCategory() {
        Category updated = new Category();
        updated.setName("Updated");
        updated.setDescription("Updated desc");
        updated.setIcon("ri-folder-line");
        updated.setColor("#000000");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        Category result = categoryService.updateCategory(1L, updated);
        assertNotNull(result);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    public void testDeleteCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        doNothing().when(categoryRepository).delete(any(Category.class));
        categoryService.deleteCategory(1L);
        verify(categoryRepository, times(1)).delete(any(Category.class));
    }
}
