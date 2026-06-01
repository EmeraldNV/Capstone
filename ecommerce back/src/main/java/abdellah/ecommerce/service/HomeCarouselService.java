package abdellah.ecommerce.service;

import abdellah.ecommerce.api.dto.home.HomeCarouselImageResponse;
import abdellah.ecommerce.domain.entity.HomeCarouselImage;
import abdellah.ecommerce.exception.BadRequestApiException;
import abdellah.ecommerce.exception.NotFoundApiException;
import abdellah.ecommerce.repository.HomeCarouselImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class HomeCarouselService {

    private final HomeCarouselImageRepository repository;
    private final ProductMediaService mediaService;

    public HomeCarouselService(HomeCarouselImageRepository repository, ProductMediaService mediaService) {
        this.repository = repository;
        this.mediaService = mediaService;
    }

    @Transactional(readOnly = true)
    public List<HomeCarouselImageResponse> listPublicImages() {
        return repository.findAllByActiveTrueOrderBySortOrderAscCreatedAtAsc().stream()
                .map(HomeCarouselService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HomeCarouselImageResponse> listAdminImages() {
        return repository.findAllByOrderBySortOrderAscCreatedAtAsc().stream()
                .map(HomeCarouselService::toResponse)
                .toList();
    }

    @Transactional
    public List<HomeCarouselImageResponse> addImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestApiException("IMAGES_REQUIRED", "At least one image must be provided.");
        }

        int nextSortOrder = repository.findAllByOrderBySortOrderAscCreatedAtAsc().stream()
                .map(HomeCarouselImage::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        List<HomeCarouselImage> createdImages = new ArrayList<>(files.size());
        int index = 0;
        for (MultipartFile file : files) {
            String imageUrl = mediaService.storeHomeCarouselImage(file);
            HomeCarouselImage image = new HomeCarouselImage();
            image.setImageUrl(imageUrl);
            image.setAltText(buildAltText(file));
            image.setSortOrder(nextSortOrder + index);
            image.setActive(Boolean.TRUE);
            createdImages.add(image);
            index++;
        }

        repository.saveAll(createdImages);
        return listAdminImages();
    }

    @Transactional
    public List<HomeCarouselImageResponse> reorderImages(List<Long> orderedImageIds) {
        if (orderedImageIds == null || orderedImageIds.isEmpty()) {
            throw new BadRequestApiException("IMAGE_ORDER_REQUIRED", "At least one image must be provided.");
        }

        List<HomeCarouselImage> currentImages = repository.findAllByOrderBySortOrderAscCreatedAtAsc();
        if (currentImages.size() != orderedImageIds.size()) {
            throw new BadRequestApiException("IMAGE_ORDER_MISMATCH", "Image order does not match the current carousel images.");
        }

        Set<Long> currentIds = currentImages.stream().map(HomeCarouselImage::getId).collect(java.util.stream.Collectors.toSet());
        Set<Long> requestedIds = new HashSet<>(orderedImageIds);
        if (currentIds.size() != requestedIds.size() || !currentIds.containsAll(requestedIds)) {
            throw new BadRequestApiException("IMAGE_ORDER_INVALID", "Image order contains invalid or duplicate image ids.");
        }

        List<HomeCarouselImage> reordered = new ArrayList<>(orderedImageIds.size());
        for (Long imageId : orderedImageIds) {
            HomeCarouselImage image = currentImages.stream()
                    .filter(candidate -> imageId.equals(candidate.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestApiException("IMAGE_ORDER_INVALID", "Image order contains invalid or duplicate image ids."));
            reordered.add(image);
        }

        applyImageOrder(reordered);
        repository.saveAll(reordered);
        return listAdminImages();
    }

    @Transactional
    public List<HomeCarouselImageResponse> removeImage(Long imageId) {
        HomeCarouselImage image = repository.findById(imageId)
                .orElseThrow(() -> new NotFoundApiException("HOME_CAROUSEL_IMAGE_NOT_FOUND", "Carousel image not found."));
        mediaService.deleteStoredImage(image.getImageUrl());
        repository.delete(image);
        normalizeImageOrder();
        return listAdminImages();
    }

    private void normalizeImageOrder() {
        List<HomeCarouselImage> ordered = repository.findAllByOrderBySortOrderAscCreatedAtAsc();
        applyImageOrder(ordered);
        repository.saveAll(ordered);
    }

    private void applyImageOrder(List<HomeCarouselImage> orderedImages) {
        int sortOrder = 0;
        for (HomeCarouselImage image : orderedImages) {
            image.setSortOrder(sortOrder++);
        }
    }

    private static HomeCarouselImageResponse toResponse(HomeCarouselImage image) {
        return new HomeCarouselImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getAltText(),
                image.getSortOrder(),
                image.getActive(),
                image.getCreatedAt(),
                image.getUpdatedAt()
        );
    }

    private String buildAltText(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            return "Home carousel image";
        }
        return originalName.trim();
    }
}
