package abdellah.ecommerce.bootstrap;

import abdellah.ecommerce.domain.entity.Category;
import abdellah.ecommerce.repository.CategoryRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CategoryBootstrapRunner implements ApplicationRunner {

    private static final Set<String> LEGACY_ROOT_NAMES = Set.of(
            "ADULT", "ADULTS", "KID", "KIDS", "MEN", "WOMEN", "UNISEX", "JUNIOR", "BABY"
    );

    private final CategoryRepository categoryRepository;

    public CategoryBootstrapRunner(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Category footwear = upsertRoot("FOOTWEAR", "Footwear", "footwear", 10);
        Category apparel = upsertRoot("APPAREL", "Apparel", "apparel", 20);

        upsertChild("SHOES", "Shoes", "shoes", footwear, 10);
        upsertChild("SOCKS", "Socks", "socks", footwear, 20);
        upsertChild("TSHIRT", "T-Shirt", "tshirt", apparel, 10);
        upsertChild("CROP_TOP", "Crop Top", "crop-top", apparel, 20);

        deactivateLegacyRoots();
    }

    private Category upsertRoot(String code, String name, String slug, int sortOrder) {
        Category category = categoryRepository.findByCategoryCodeIgnoreCase(code).orElseGet(Category::new);
        category.setCategoryCode(code);
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(null);
        category.setParent(null);
        category.setSortOrder(sortOrder);
        category.setActive(Boolean.TRUE);
        return categoryRepository.save(category);
    }

    private Category upsertChild(String code, String name, String slug, Category parent, int sortOrder) {
        Category category = categoryRepository.findByCategoryCodeIgnoreCase(code).orElseGet(Category::new);
        category.setCategoryCode(code);
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(null);
        category.setParent(parent);
        category.setSortOrder(sortOrder);
        category.setActive(Boolean.TRUE);
        return categoryRepository.save(category);
    }

    private void deactivateLegacyRoots() {
        List<Category> legacyRoots = categoryRepository.findAll().stream()
                .filter(category -> category.getParent() == null)
                .filter(category -> category.getActive() == null || Boolean.TRUE.equals(category.getActive()))
                .filter(category -> LEGACY_ROOT_NAMES.contains(normalize(category.getCategoryCode()))
                        || LEGACY_ROOT_NAMES.contains(normalize(category.getName())))
                .toList();

        for (Category legacyRoot : legacyRoots) {
            legacyRoot.setActive(Boolean.FALSE);
            categoryRepository.save(legacyRoot);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
