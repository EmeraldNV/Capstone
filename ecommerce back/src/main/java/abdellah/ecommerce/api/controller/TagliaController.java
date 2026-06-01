package abdellah.ecommerce.api.controller;

import abdellah.ecommerce.api.dto.taglia.TagliaRequest;
import abdellah.ecommerce.api.dto.taglia.TagliaResponse;
import abdellah.ecommerce.service.TagliaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Tag(name = "Taglie", description = "Gestione delle taglie disponibili per il catalogo")
@RestController
@RequestMapping({"/api/taglie", "/api/v1/taglie"})
public class TagliaController {

    private final TagliaService tagliaService;

    public TagliaController(TagliaService tagliaService) {
        this.tagliaService = tagliaService;
    }

    @Operation(summary = "Recupera tutte le taglie disponibili")
    @ApiResponse(responseCode = "200", description = "Elenco taglie restituito correttamente")
    @GetMapping
    public ResponseEntity<List<TagliaResponse>> listTaglie() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.MINUTES).cachePublic())
                .body(tagliaService.listTaglie());
    }

    @Operation(summary = "Crea una nuova taglia")
    @ApiResponse(responseCode = "201", description = "Taglia creata correttamente")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping
    public ResponseEntity<TagliaResponse> create(@Valid @RequestBody TagliaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagliaService.createTaglia(request));
    }

    @Operation(summary = "Aggiorna una taglia esistente")
    @ApiResponse(responseCode = "200", description = "Taglia aggiornata correttamente")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<TagliaResponse> update(@PathVariable Long id, @Valid @RequestBody TagliaRequest request) {
        return ResponseEntity.ok(tagliaService.updateTaglia(id, request));
    }

    @Operation(summary = "Elimina una taglia")
    @ApiResponse(responseCode = "204", description = "Taglia eliminata correttamente")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagliaService.deleteTaglia(id);
        return ResponseEntity.noContent().build();
    }
}
