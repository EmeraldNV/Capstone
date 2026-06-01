package abdellah.ecommerce.service;

import abdellah.ecommerce.api.dto.category.CategoryNavigationResponse;
import abdellah.ecommerce.domain.entity.Category;
import abdellah.ecommerce.exception.NotFoundApiException;
import abdellah.ecommerce.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CategoryService {

    private static final Set<String> NAVIGATION_ROOT_CODES = Set.of("FOOTWEAR", "APPAREL");

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryNavigationResponse> listNavigationTree() {
        List<Category> categories = categoryRepository.findAllByActiveTrueOrderBySortOrderAscNameAsc();
        Map<Long, CategoryNode> nodes = new HashMap<>();
        List<CategoryNode> roots = new ArrayList<>();

        for (Category category : categories) {
            CategoryNode node = nodes.computeIfAbsent(category.getId(), id -> new CategoryNode(category));
            if (category.getParent() == null) {
                if (NAVIGATION_ROOT_CODES.contains(normalize(category.getCategoryCode()))) {
                    roots.add(node);
                }
                continue;
            }
            CategoryNode parentNode = nodes.computeIfAbsent(category.getParent().getId(),
                    id -> new CategoryNode(category.getParent()));
            parentNode.children.add(node);
        }

        roots.sort(CategoryNode::compareTo);
        for (CategoryNode root : roots) {
            root.sortChildren(new HashSet<>());
        }

        return roots.stream().map(root -> root.toResponse(new HashSet<>())).toList();
    }

    @Transactional(readOnly = true)
    public Set<Long> allowedNavigationCategoryIds() {
        Set<Long> ids = new LinkedHashSet<>();
        for (CategoryNavigationResponse root : listNavigationTree()) {
            collectNavigationIds(root, ids);
        }
        return ids;
    }

    @Transactional(readOnly = true)
    public Set<Long> resolveCategoryScopeIds(String categorySlug) {
        if (categorySlug == null || categorySlug.isBlank()) {
            return Set.of();
        }

        Category root = categoryRepository.findBySlugIgnoreCaseAndActiveTrue(categorySlug.trim())
                .orElseThrow(() -> new NotFoundApiException("CATEGORY_NOT_FOUND", "Category not found."));
        if (!allowedNavigationCategoryIds().contains(root.getId())) {
            throw new NotFoundApiException("CATEGORY_NOT_FOUND", "Category not found.");
        }
        Set<Long> ids = new LinkedHashSet<>();
        collectCategoryIds(root, ids, new HashSet<>());
        return ids;
    }

    private void collectCategoryIds(Category category, Set<Long> ids, Set<Long> visited) {
        if (!visited.add(category.getId())) {
            return;
        }
        ids.add(category.getId());
        for (Category child : category.getChildren()) {
            if (Boolean.TRUE.equals(child.getActive())) {
                collectCategoryIds(child, ids, visited);
            }
        }
    }

    private void collectNavigationIds(CategoryNavigationResponse category, Set<Long> ids) {
        collectNavigationIds(category, ids, new HashSet<>());
    }

    private void collectNavigationIds(CategoryNavigationResponse category, Set<Long> ids, Set<Long> visited) {
        if (!visited.add(category.id())) {
            return;
        }
        ids.add(category.id());
        for (CategoryNavigationResponse child : category.children()) {
            collectNavigationIds(child, ids, visited);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static final class CategoryNode implements Comparable<CategoryNode> {
        private final Category category;
        private final List<CategoryNode> children = new ArrayList<>();

        private CategoryNode(Category category) {
            this.category = category;
        }

        private void sortChildren(Set<Long> visiting) {
            if (!visiting.add(category.getId())) {
                return;
            }
            children.sort(CategoryNode::compareTo);
            List<CategoryNode> safeChildren = new ArrayList<>();
            for (CategoryNode child : children) {
                if (visiting.contains(child.category.getId())) {
                    continue;
                }
                child.sortChildren(new HashSet<>(visiting));
                safeChildren.add(child);
            }
            children.clear();
            children.addAll(safeChildren);
        }

        private CategoryNavigationResponse toResponse(Set<Long> visiting) {
            if (!visiting.add(category.getId())) {
                return new CategoryNavigationResponse(
                        category.getId(),
                        category.getCategoryCode(),
                        category.getName(),
                        category.getSlug(),
                        category.getDescription(),
                        category.getSortOrder(),
                        category.getParent() == null ? null : category.getParent().getId(),
                        category.getParent() == null ? null : category.getParent().getSlug(),
                        List.of()
                );
            }
            return new CategoryNavigationResponse(
                    category.getId(),
                    category.getCategoryCode(),
                    category.getName(),
                    category.getSlug(),
                    category.getDescription(),
                    category.getSortOrder(),
                    category.getParent() == null ? null : category.getParent().getId(),
                    category.getParent() == null ? null : category.getParent().getSlug(),
                    children.stream()
                            .filter(child -> !visiting.contains(child.category.getId()))
                            .map(child -> child.toResponse(new HashSet<>(visiting)))
                            .toList()
            );
        }

        @Override
        public int compareTo(CategoryNode other) {
            int orderComparison = Integer.compare(
                    category.getSortOrder() == null ? Integer.MAX_VALUE : category.getSortOrder(),
                    other.category.getSortOrder() == null ? Integer.MAX_VALUE : other.category.getSortOrder()
            );
            if (orderComparison != 0) {
                return orderComparison;
            }
            return category.getName().toLowerCase(Locale.ROOT)
                    .compareTo(other.category.getName().toLowerCase(Locale.ROOT));
        }
    }
}
