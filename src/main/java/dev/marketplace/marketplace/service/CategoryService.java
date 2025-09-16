package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.Category;
import dev.marketplace.marketplace.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category findById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
    }

    public List<UUID> getAllDescendantCategoryIds(UUID parentId) {
        Category parent = findById(parentId);
        List<UUID> ids = new ArrayList<>();
        collectDescendantIds(parent, ids);
        return ids;
    }

    private void collectDescendantIds(Category category, List<UUID> ids) {
        ids.add(category.getId());
        for (Category child : category.getChildren()) {
            collectDescendantIds(child, ids);
        }
    }
}
