package abdellah.ecommerce.service;

import abdellah.ecommerce.api.dto.product.ProductDetailResponse;
import abdellah.ecommerce.api.dto.product.ProductImageResponse;
import abdellah.ecommerce.api.dto.product.ProductSummaryResponse;
import abdellah.ecommerce.api.dto.taglia.TagliaResponse;
import abdellah.ecommerce.domain.entity.Product;
import abdellah.ecommerce.domain.entity.ProductImage;
import abdellah.ecommerce.domain.entity.Taglia;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public final class ProductResponseMapper {

    private ProductResponseMapper() {
    }

    public static ProductSummaryResponse toSummary(Product product) {
        List<ProductImageResponse> images = product.getImages().stream()
                .sorted(imageComparator())
                .map(ProductResponseMapper::toImageResponse)
                .toList();
        return new ProductSummaryResponse(
                product.getId(),
                product.getProductCode(),
                product.getSlug(),
                product.getName(),
                product.getShortDescription(),
                product.getListPrice(),
                product.getSalePrice(),
                effectivePrice(product),
                product.getCurrencyCode(),
                product.getStockQuantity(),
                product.getActive(),
                images.isEmpty() ? null : images.get(0).imageUrl(),
                product.getBrand() == null ? null : product.getBrand().getName(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.getCategory() == null ? null : product.getCategory().getSlug(),
                product.getIpfApproved(),
                product.getTaglie().stream()
                        .sorted(tagliaComparator())
                        .map(ProductResponseMapper::toTagliaResponse)
                        .toList(),
                images,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public static ProductDetailResponse toDetail(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getProductCode(),
                product.getSlug(),
                product.getName(),
                product.getShortDescription(),
                product.getDescription(),
                product.getSportType(),
                product.getGender() == null ? null : product.getGender().name(),
                product.getAgeGroup() == null ? null : product.getAgeGroup().name(),
                product.getSeason() == null ? null : product.getSeason().name(),
                product.getMaterial(),
                product.getCareInstructions(),
                product.getTaxable(),
                product.getIpfApproved(),
                product.getActive(),
                product.getListPrice(),
                product.getSalePrice(),
                effectivePrice(product),
                product.getCurrencyCode(),
                product.getStockQuantity(),
                product.getBrand() == null ? null : product.getBrand().getId(),
                product.getBrand() == null ? null : product.getBrand().getName(),
                product.getCategory() == null ? null : product.getCategory().getId(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.getCategory() == null ? null : product.getCategory().getSlug(),
                product.getTaglie().stream()
                        .sorted(tagliaComparator())
                        .map(ProductResponseMapper::toTagliaResponse)
                        .toList(),
                product.getImages().stream()
                        .sorted(imageComparator())
                        .map(ProductResponseMapper::toImageResponse)
                        .toList(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private static ProductImageResponse toImageResponse(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getAltText(),
                image.getPrimary(),
                image.getSortOrder()
        );
    }

    private static TagliaResponse toTagliaResponse(Taglia taglia) {
        return new TagliaResponse(
                taglia.getId(),
                taglia.getNome(),
                taglia.getCodice(),
                taglia.getCreatedAt(),
                taglia.getUpdatedAt()
        );
    }

    private static Comparator<ProductImage> imageComparator() {
        return Comparator
                .comparing((ProductImage image) -> !Boolean.TRUE.equals(image.getPrimary()))
                .thenComparing(ProductImage::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProductImage::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProductImage::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static BigDecimal effectivePrice(Product product) {
        return product.getSalePrice() != null ? product.getSalePrice() : product.getListPrice();
    }

    private static java.util.Comparator<Taglia> tagliaComparator() {
        return java.util.Comparator
                .comparing(Taglia::getNome, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Taglia::getCodice, String.CASE_INSENSITIVE_ORDER);
    }
}
