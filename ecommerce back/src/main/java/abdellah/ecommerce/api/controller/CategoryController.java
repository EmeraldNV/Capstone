package abdellah.ecommerce.api.controller;

import abdellah.ecommerce.api.dto.category.CategoryNavigationResponse;
import abdellah.ecommerce.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/navigation")
    public ResponseEntity<List<CategoryNavigationResponse>> listNavigation() {
        return ResponseEntity.ok(categoryService.listNavigationTree());
    }
}
