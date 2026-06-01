package abdellah.ecommerce.api.controller;

import abdellah.ecommerce.api.dto.product.ProductDetailResponse;
import abdellah.ecommerce.api.dto.product.ProductSummaryResponse;
import abdellah.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Prodotti pubblici", description = "Endpoint pubblici per il catalogo prodotti")
@RestController
@RequestMapping("/api/prodotti")
public class ProdottiController {

    private final ProductService productService;

    public ProdottiController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Recupera i prodotti filtrati per taglia")
    @ApiResponse(responseCode = "200", description = "Pagina prodotti restituita correttamente")
    @GetMapping
    public ResponseEntity<Page<ProductSummaryResponse>> listProductsByTaglia(
            @RequestParam(name = "taglia", required = false) Long tagliaId,
            Pageable pageable) {
        return ResponseEntity.ok(productService.listPublicProductsByTaglia(tagliaId, pageable));
    }

    @Operation(summary = "Recupera il dettaglio di un prodotto tramite slug")
    @ApiResponse(responseCode = "200", description = "Prodotto restituito correttamente")
    @GetMapping("/{slug}")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getPublicProductBySlug(slug));
    }
}
