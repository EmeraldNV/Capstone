package abdellah.ecommerce.service;

import abdellah.ecommerce.config.ProductMediaProperties;
import abdellah.ecommerce.exception.BadRequestApiException;
import abdellah.ecommerce.exception.NotFoundApiException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductMediaService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final Path storageRoot;

    public ProductMediaService(ProductMediaProperties properties) {
        this.storageRoot = Paths.get(properties.getStoragePath()).toAbsolutePath().normalize();
    }

    @Transactional
    public String storeProductImage(Long productId, MultipartFile file) {
        validateImage(file);
        try {
            Path productDirectory = storageRoot.resolve("products").resolve(String.valueOf(productId)).normalize();
            Files.createDirectories(productDirectory);
            String extension = resolveExtension(file);
            String storedFileName = UUID.randomUUID() + extension;
            Path target = productDirectory.resolve(storedFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/products/" + productId + "/" + storedFileName;
        } catch (IOException ex) {
            throw new BadRequestApiException("IMAGE_UPLOAD_FAILED", "The image could not be stored.");
        }
    }

    @Transactional
    public String storeHomeCarouselImage(MultipartFile file) {
        validateImage(file);
        try {
            Path carouselDirectory = storageRoot.resolve("home-carousel").normalize();
            Files.createDirectories(carouselDirectory);
            String extension = resolveExtension(file);
            String storedFileName = UUID.randomUUID() + extension;
            Path target = carouselDirectory.resolve(storedFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/home-carousel/" + storedFileName;
        } catch (IOException ex) {
            throw new BadRequestApiException("IMAGE_UPLOAD_FAILED", "The image could not be stored.");
        }
    }

    @Transactional(readOnly = true)
    public Resource loadResource(String relativePath) {
        try {
            Path resolved = resolvePath(relativePath);
            Resource resource = new UrlResource(resolved.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundApiException("MEDIA_NOT_FOUND", "Requested media file was not found.");
            }
            return resource;
        } catch (IOException ex) {
            throw new NotFoundApiException("MEDIA_NOT_FOUND", "Requested media file was not found.");
        }
    }

    @Transactional
    public void deleteProductDirectory(Long productId) {
        Path productDirectory = storageRoot.resolve("products").resolve(String.valueOf(productId)).normalize();
        deleteIfExists(productDirectory);
    }

    @Transactional
    public void deleteStoredImage(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) {
            return;
        }
        Path path = resolvePath(imageUrl.substring("/uploads/".length()));
        deleteIfExists(path);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestApiException("IMAGE_REQUIRED", "One or more images are empty.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestApiException("INVALID_IMAGE_TYPE", "Only JPG, PNG, WEBP and GIF images are allowed.");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            return switch (file.getContentType() == null ? "" : file.getContentType().toLowerCase()) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                case "image/gif" -> ".gif";
                default -> ".bin";
            };
        }
        String extension = originalName.substring(originalName.lastIndexOf('.'));
        return extension.length() <= 10 ? extension : ".bin";
    }

    private Path resolvePath(String relativePath) {
        Path resolved = storageRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new BadRequestApiException("INVALID_MEDIA_PATH", "Invalid media path.");
        }
        return resolved;
    }

    private void deleteIfExists(Path path) {
        try {
            if (!Files.exists(path)) {
                return;
            }
            try (var stream = Files.walk(path)) {
                stream.sorted((left, right) -> right.compareTo(left))
                        .forEach(candidate -> {
                            try {
                                Files.deleteIfExists(candidate);
                            } catch (IOException ignored) {
                            }
                        });
            }
        } catch (IOException ignored) {
        }
    }
}
