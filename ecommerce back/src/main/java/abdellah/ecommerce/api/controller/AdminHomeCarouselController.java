package abdellah.ecommerce.api.controller;

import abdellah.ecommerce.api.dto.home.HomeCarouselImageOrderRequest;
import abdellah.ecommerce.api.dto.home.HomeCarouselImageResponse;
import abdellah.ecommerce.service.HomeCarouselService;
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
@RequestMapping("/api/v1/admin/home-carousel")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminHomeCarouselController {

    private final HomeCarouselService service;

    public AdminHomeCarouselController(HomeCarouselService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<HomeCarouselImageResponse>> listImages() {
        return ResponseEntity.ok(service.listAdminImages());
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<HomeCarouselImageResponse>> uploadImages(@RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addImages(files));
    }

    @PutMapping("/images/order")
    public ResponseEntity<List<HomeCarouselImageResponse>> reorderImages(@Valid @RequestBody HomeCarouselImageOrderRequest request) {
        return ResponseEntity.ok(service.reorderImages(request.imageIds()));
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<List<HomeCarouselImageResponse>> deleteImage(@PathVariable Long imageId) {
        return ResponseEntity.ok(service.removeImage(imageId));
    }
}
