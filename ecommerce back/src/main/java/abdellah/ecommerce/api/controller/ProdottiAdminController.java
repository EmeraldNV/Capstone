package abdellah.ecommerce.api.controller;

import abdellah.ecommerce.api.dto.product.ProductDetailResponse;
import abdellah.ecommerce.api.dto.product.ProductImageOrderRequest;
import abdellah.ecommerce.api.dto.product.ProductUpsertRequest;
import abdellah.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Prodotti amministrativi", description = "Gestione amministrativa dei prodotti")
@RestController
@RequestMapping("/api/prodotti")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class ProdottiAdminController {

    private final ProductService productService;

    public ProdottiAdminController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Crea un prodotto con taglie associate")
    @ApiResponse(responseCode = "201", description = "Prodotto creato correttamente")
    @PostMapping
    public ResponseEntity<ProductDetailResponse> create(@Valid @RequestBody ProductUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @Operation(summary = "Aggiorna un prodotto e le sue taglie")
    @ApiResponse(responseCode = "200", description = "Prodotto aggiornato correttamente")
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
