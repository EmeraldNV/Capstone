package abdellah.ecommerce.api.controller;

import abdellah.ecommerce.api.dto.home.HomeCarouselImageResponse;
import abdellah.ecommerce.service.HomeCarouselService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/home-carousel")
public class HomeCarouselController {

    private final HomeCarouselService service;

    public HomeCarouselController(HomeCarouselService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<HomeCarouselImageResponse>> listPublicImages() {
        return ResponseEntity.ok(service.listPublicImages());
    }
}
