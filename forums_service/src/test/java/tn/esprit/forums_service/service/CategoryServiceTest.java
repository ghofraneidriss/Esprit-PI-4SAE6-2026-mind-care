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
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Health");
        category.setDescription("Health related topics");
        category.setIcon("ri-heart-line");
        category.setColor("#ff0000");
    }

    // ✅ Test 1 : createCategory avec icon et color fournis
    @Test
    void testCreateCategory_withIconAndColor() {
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        Category result = categoryService.createCategory(category);

        assertNotNull(result);
        assertEquals("Health", result.getName());
        assertEquals("ri-heart-line", result.getIcon());
        assertEquals("#ff0000", result.getColor());
        verify(categoryRepository, times(1)).save(category);
    }

    // ✅ Test 2 : createCategory sans icon → doit mettre icon par défaut
    @Test
    void testCreateCategory_withoutIcon_setsDefault() {
        category.setIcon(null);
        category.setColor(null);

        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        categoryService.createCategory(category);

        assertEquals("ri-folder-line", category.getIcon());
        assertEquals("#6366f1", category.getColor());
    }

    // ✅ Test 3 : getAllCategories retourne la liste
    @Test
    void testGetAllCategories() {
        Category cat2 = new Category();
        cat2.setId(2L);
        cat2.setName("Memory");

        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category, cat2));

        List<Category> result = categoryService.getAllCategories();

        assertEquals(2, result.size());
        verify(categoryRepository, times(1)).findAll();
    }

    // ✅ Test 4 : getAllCategoryDtos retourne des DTOs
    @Test
    void testGetAllCategoryDtos() {
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category));

        List<CategoryDTO> dtos = categoryService.getAllCategoryDtos();

        assertEquals(1, dtos.size());
        assertEquals("Health", dtos.get(0).getName());
        assertEquals("ri-heart-line", dtos.get(0).getIcon());
    }

    // ✅ Test 5 : getCategoryById trouvé
    @Test
    void testGetCategoryById_found() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Category result = categoryService.getCategoryById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    // ✅ Test 6 : getCategoryById non trouvé → exception
    @Test
    void testGetCategoryById_notFound_throwsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.getCategoryById(99L));
    }

    // ✅ Test 7 : updateCategory
    @Test
    void testUpdateCategory() {
        Category updated = new Category();
        updated.setName("Updated");
        updated.setDescription("Updated desc");
        updated.setIcon("ri-new-icon");
        updated.setColor("#123456");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        Category result = categoryService.updateCategory(1L, updated);

        assertNotNull(result);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    // ✅ Test 8 : deleteCategory
    @Test
    void testDeleteCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        doNothing().when(categoryRepository).delete(category);

        categoryService.deleteCategory(1L);

        verify(categoryRepository, times(1)).delete(category);
    }
}
