package abdellah.ecommerce.api.controller;

import abdellah.ecommerce.api.dto.product.ProductDetailResponse;
import abdellah.ecommerce.api.dto.product.ProductSummaryResponse;
import abdellah.ecommerce.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductSummaryResponse>> listPublicProducts(
            @RequestParam(name = "category", required = false) String categorySlug) {
        return ResponseEntity.ok(productService.listPublicProducts(categorySlug));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProductDetailResponse> getPublicProduct(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getPublicProductBySlug(slug));
    }
}
