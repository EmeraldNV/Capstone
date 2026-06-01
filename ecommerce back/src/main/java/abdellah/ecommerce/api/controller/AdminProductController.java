package abdellah.ecommerce.api.controller;

import abdellah.ecommerce.api.dto.product.ProductDetailResponse;
import abdellah.ecommerce.api.dto.product.ProductImageOrderRequest;
import abdellah.ecommerce.api.dto.product.ProductSummaryResponse;
import abdellah.ecommerce.api.dto.product.ProductUpsertRequest;
import abdellah.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductSummaryResponse>> listProducts() {
        return ResponseEntity.ok(productService.listAdminProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getAdminProduct(id));
    }

    @PostMapping
    public ResponseEntity<ProductDetailResponse> create(@Valid @RequestBody ProductUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody ProductUpsertRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(productService.deleteProduct(id));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDetailResponse> uploadImages(@PathVariable Long id,
                                                              @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.ok(productService.addImages(id, files));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<ProductDetailResponse> deleteImage(@PathVariable Long id,
                                                             @PathVariable Long imageId) {
        return ResponseEntity.ok(productService.removeImage(id, imageId));
    }

    @PostMapping("/{id}/images/{imageId}/primary")
    public ResponseEntity<ProductDetailResponse> setPrimaryImage(@PathVariable Long id,
                                                                 @PathVariable Long imageId) {
        return ResponseEntity.ok(productService.setPrimaryImage(id, imageId));
    }

    @PutMapping("/{id}/images/order")
    public ResponseEntity<ProductDetailResponse> reorderImages(@PathVariable Long id,
                                                               @Valid @RequestBody ProductImageOrderRequest request) {
        return ResponseEntity.ok(productService.reorderImages(id, request.imageIds()));
    }
}
