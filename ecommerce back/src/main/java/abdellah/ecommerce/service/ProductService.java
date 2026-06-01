package abdellah.ecommerce.service;

import abdellah.ecommerce.api.dto.product.ProductDetailResponse;
import abdellah.ecommerce.api.dto.product.ProductImageOrderRequest;
import abdellah.ecommerce.api.dto.product.ProductSummaryResponse;
import abdellah.ecommerce.api.dto.product.ProductUpsertRequest;
import abdellah.ecommerce.domain.entity.Brand;
import abdellah.ecommerce.domain.entity.Category;
import abdellah.ecommerce.domain.entity.Product;
import abdellah.ecommerce.domain.entity.ProductImage;
import abdellah.ecommerce.domain.entity.Taglia;
import abdellah.ecommerce.service.CategoryService;
import abdellah.ecommerce.domain.enums.ProductClassification;
import abdellah.ecommerce.exception.BadRequestApiException;
import abdellah.ecommerce.exception.NotFoundApiException;
import abdellah.ecommerce.repository.BrandRepository;
import abdellah.ecommerce.repository.CategoryRepository;
import abdellah.ecommerce.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final ProductMediaService productMediaService;
    private final TagliaService tagliaService;

    public ProductService(ProductRepository productRepository,
                          BrandRepository brandRepository,
                          CategoryRepository categoryRepository,
                          CategoryService categoryService,
                          ProductMediaService productMediaService,
                          TagliaService tagliaService) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.categoryService = categoryService;
        this.productMediaService = productMediaService;
        this.tagliaService = tagliaService;
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> listPublicProducts(String categorySlug) {
        Set<Long> categoryScopeIds = hasText(categorySlug)
                ? categoryService.resolveCategoryScopeIds(categorySlug)
                : null;
        return productRepository.findAllByActiveTrueOrderByCreatedAtDesc().stream()
                .filter(product -> matchesCategoryFilter(product, categoryScopeIds))
                .map(ProductResponseMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> listAdminProducts() {
        return productRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ProductResponseMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> listPublicProductsByTaglia(Long tagliaId, Pageable pageable) {
        Page<Product> products;
        if (tagliaId == null) {
            products = productRepository.findAllByActiveTrueOrderByCreatedAtDesc(pageable);
        } else {
            tagliaService.getTagliaEntity(tagliaId);
            products = productRepository.findDistinctByTaglie_IdAndActiveTrueOrderByCreatedAtDesc(tagliaId, pageable);
        }
        return products.map(ProductResponseMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getPublicProductBySlug(String slug) {
        Product product = productRepository.findWithDetailsBySlugIgnoreCase(slug)
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new NotFoundApiException("PRODUCT_NOT_FOUND", "Product not found."));
        return ProductResponseMapper.toDetail(product);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getAdminProduct(Long id) {
        return ProductResponseMapper.toDetail(loadProductWithDetails(id));
    }

    @Transactional
    public ProductDetailResponse createProduct(ProductUpsertRequest request) {
        Product product = new Product();
        applyRequest(product, request, null);
        product = productRepository.saveAndFlush(product);
        return ProductResponseMapper.toDetail(loadProductWithDetails(product.getId()));
    }

    @Transactional
    public ProductDetailResponse updateProduct(Long id, ProductUpsertRequest request) {
        Product product = loadProductWithDetails(id);
        applyRequest(product, request, id);
        product = productRepository.saveAndFlush(product);
        return ProductResponseMapper.toDetail(loadProductWithDetails(product.getId()));
    }

    @Transactional
    public ProductDetailResponse deleteProduct(Long id) {
        Product product = loadProductWithDetails(id);
        productMediaService.deleteProductDirectory(product.getId());
        productRepository.delete(product);
        return ProductResponseMapper.toDetail(product);
    }

    @Transactional
    public ProductDetailResponse addImages(Long productId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestApiException("IMAGES_REQUIRED", "At least one image must be provided.");
        }
        Product product = loadProductWithDetails(productId);
        int nextSortOrder = product.getImages().stream()
                .map(ProductImage::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        boolean hasPrimary = product.getImages().stream().anyMatch(image -> Boolean.TRUE.equals(image.getPrimary()));
        int index = 0;
        for (MultipartFile file : files) {
            String imageUrl = productMediaService.storeProductImage(productId, file);
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(imageUrl);
            image.setAltText(buildAltText(product.getName(), file));
            image.setSortOrder(nextSortOrder + index);
            image.setPrimary(!hasPrimary && index == 0);
            product.getImages().add(image);
            index++;
        }
        product = productRepository.saveAndFlush(product);
        return ProductResponseMapper.toDetail(loadProductWithDetails(productId));
    }

    @Transactional
    public ProductDetailResponse removeImage(Long productId, Long imageId) {
        Product product = loadProductWithDetails(productId);
        ProductImage image = product.getImages().stream()
                .filter(candidate -> imageId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundApiException("PRODUCT_IMAGE_NOT_FOUND", "Product image not found."));
        boolean wasPrimary = Boolean.TRUE.equals(image.getPrimary());
        product.getImages().remove(image);
        productMediaService.deleteStoredImage(image.getImageUrl());
        normalizeImageOrder(product);
        productRepository.saveAndFlush(product);
        if (wasPrimary && !product.getImages().isEmpty()) {
            ProductImage replacement = product.getImages().stream()
                    .sorted(imageOrderComparator())
                    .findFirst()
                    .orElse(null);
            if (replacement != null) {
                replacement.setPrimary(Boolean.TRUE);
                productRepository.saveAndFlush(product);
            }
        }
        return ProductResponseMapper.toDetail(loadProductWithDetails(productId));
    }

    @Transactional
    public ProductDetailResponse setPrimaryImage(Long productId, Long imageId) {
        Product product = loadProductWithDetails(productId);
        ProductImage target = null;
        for (ProductImage image : product.getImages()) {
            boolean selected = imageId.equals(image.getId());
            image.setPrimary(selected);
            if (selected) {
                target = image;
            }
        }
        if (target == null) {
            throw new NotFoundApiException("PRODUCT_IMAGE_NOT_FOUND", "Product image not found.");
        }
        productRepository.saveAndFlush(product);
        return ProductResponseMapper.toDetail(loadProductWithDetails(productId));
    }

    @Transactional
    public ProductDetailResponse reorderImages(Long productId, List<Long> orderedImageIds) {
        if (orderedImageIds == null || orderedImageIds.isEmpty()) {
            throw new BadRequestApiException("IMAGE_ORDER_REQUIRED", "At least one image must be provided.");
        }

        Product product = loadProductWithDetails(productId);
        if (product.getImages().size() != orderedImageIds.size()) {
            throw new BadRequestApiException("IMAGE_ORDER_MISMATCH", "Image order does not match the current product images.");
        }

        Set<Long> currentIds = product.getImages().stream()
                .map(ProductImage::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> requestedIds = new HashSet<>(orderedImageIds);
        if (currentIds.size() != requestedIds.size() || !currentIds.containsAll(requestedIds)) {
            throw new BadRequestApiException("IMAGE_ORDER_INVALID", "Image order contains invalid or duplicate image ids.");
        }

        List<ProductImage> reordered = new ArrayList<>(orderedImageIds.size());
        for (Long imageId : orderedImageIds) {
            ProductImage image = product.getImages().stream()
                    .filter(candidate -> imageId.equals(candidate.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestApiException("IMAGE_ORDER_INVALID", "Image order contains invalid or duplicate image ids."));
            reordered.add(image);
        }

        applyImageOrder(product, reordered);
        productRepository.saveAndFlush(product);
        return ProductResponseMapper.toDetail(loadProductWithDetails(productId));
    }

    private void applyRequest(Product product, ProductUpsertRequest request, Long currentId) {
        String productCode = hasText(request.productCode())
                ? request.productCode().trim()
                : (product.getProductCode() != null ? product.getProductCode() : generateCode(request.name()));
        String slug = hasText(request.slug())
                ? slugify(request.slug())
                : (product.getSlug() != null ? product.getSlug() : generateSlug(request.name()));

        ensureUniqueProductCode(productCode, currentId);
        ensureUniqueSlug(slug, currentId);

        product.setProductCode(productCode);
        product.setSlug(slug);
        product.setName(request.name().trim());
        product.setShortDescription(trimToNull(request.shortDescription()));
        product.setDescription(trimToNull(request.description()));
        product.setSportType(normalizeClassification(request.sportType()));
        product.setGender(request.gender());
        product.setAgeGroup(request.ageGroup());
        product.setSeason(request.season());
        product.setMaterial(trimToNull(request.material()));
        product.setCareInstructions(trimToNull(request.careInstructions()));
        product.setTaxable(Boolean.TRUE.equals(request.taxable()));
        product.setIpfApproved(Boolean.TRUE.equals(request.ipfApproved()));
        product.setActive(Boolean.TRUE.equals(request.active()));
        product.setListPrice(request.listPrice());
        product.setSalePrice(request.salePrice());
        product.setCurrencyCode(request.currencyCode().trim().toUpperCase(Locale.ROOT));
        product.setStockQuantity(request.stockQuantity());
        product.setBrand(resolveBrand(request.brandId()));
        product.setCategory(resolveCategory(request.categoryId()));
        product.getTaglie().clear();
        product.getTaglie().addAll(resolveTaglie(request.tagliaIds()));
        validatePricing(product);
    }

    private Set<Taglia> resolveTaglie(List<Long> tagliaIds) {
        return tagliaService.resolveTaglieByIds(tagliaIds);
    }

    private Brand resolveBrand(Long brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new NotFoundApiException("BRAND_NOT_FOUND", "Brand not found."));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BadRequestApiException("CATEGORY_REQUIRED", "Please select a subcategory.");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundApiException("CATEGORY_NOT_FOUND", "Category not found."));
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new NotFoundApiException("CATEGORY_NOT_FOUND", "Category not found.");
        }
        if (!category.getChildren().isEmpty()) {
            throw new BadRequestApiException("CATEGORY_NOT_LEAF", "Please select a subcategory.");
        }
        return category;
    }

    private Product loadProductWithDetails(Long id) {
        return productRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundApiException("PRODUCT_NOT_FOUND", "Product not found."));
    }

    private void ensureUniqueSlug(String slug, Long currentId) {
        boolean exists = currentId == null
                ? productRepository.existsBySlugIgnoreCase(slug)
                : productRepository.existsBySlugIgnoreCaseAndIdNot(slug, currentId);
        if (exists) {
            throw new BadRequestApiException("PRODUCT_SLUG_EXISTS", "A product with this slug already exists.");
        }
    }

    private void ensureUniqueProductCode(String productCode, Long currentId) {
        boolean exists = currentId == null
                ? productRepository.existsByProductCodeIgnoreCase(productCode)
                : productRepository.existsByProductCodeIgnoreCaseAndIdNot(productCode, currentId);
        if (exists) {
            throw new BadRequestApiException("PRODUCT_CODE_EXISTS", "A product with this code already exists.");
        }
    }

    private void validatePricing(Product product) {
        if (product.getSalePrice() != null && product.getSalePrice().compareTo(product.getListPrice()) > 0) {
            throw new BadRequestApiException("INVALID_SALE_PRICE", "Sale price cannot be greater than list price.");
        }
    }

    private String generateCode(String value) {
        String base = slugify(value).replace("-", "");
        String candidate = ("PRD-" + base).toUpperCase(Locale.ROOT);
        if (candidate.length() > 80) {
            candidate = candidate.substring(0, 80);
        }
        String uniqueCandidate = candidate;
        int suffix = 1;
        while (productRepository.existsByProductCodeIgnoreCase(uniqueCandidate)) {
            String suffixText = "-" + suffix++;
            int maxLength = 80 - suffixText.length();
            uniqueCandidate = (candidate.length() > maxLength ? candidate.substring(0, maxLength) : candidate) + suffixText;
        }
        return uniqueCandidate;
    }

    private String generateSlug(String value) {
        String base = slugify(value);
        if (base.isBlank()) {
            base = "product";
        }
        String uniqueCandidate = base;
        int suffix = 1;
        while (productRepository.existsBySlugIgnoreCase(uniqueCandidate)) {
            uniqueCandidate = base + "-" + suffix++;
        }
        return uniqueCandidate;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        return slug.length() > 150 ? slug.substring(0, 150).replaceAll("-+$", "") : slug;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeClassification(String value) {
        if (!ProductClassification.isValid(value)) {
            throw new BadRequestApiException(
                    "INVALID_PRODUCT_CLASSIFICATION",
                    "Please select a valid product category."
            );
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildAltText(String productName, MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            return productName;
        }
        return productName + " - " + fileName.trim();
    }

    private boolean matchesCategoryFilter(Product product, Set<Long> categoryScopeIds) {
        if (categoryScopeIds == null) {
            return true;
        }
        if (product.getCategory() == null) {
            return false;
        }
        return categoryScopeIds.contains(product.getCategory().getId());
    }

    private void normalizeImageOrder(Product product) {
        List<ProductImage> ordered = product.getImages().stream()
                .sorted(imageOrderComparator())
                .toList();
        applyImageOrder(product, ordered);
    }

    private void applyImageOrder(Product product, List<ProductImage> orderedImages) {
        int sortOrder = 0;
        for (ProductImage image : orderedImages) {
            image.setSortOrder(sortOrder++);
        }
    }

    private Comparator<ProductImage> imageOrderComparator() {
        return Comparator
                .comparing(ProductImage::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProductImage::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProductImage::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}
