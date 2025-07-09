package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.Category;
import dev.marketplace.marketplace.service.CategoryService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:3000")
@Controller
public class CategoryQueryResolver {

    private final CategoryService categoryService;

    public CategoryQueryResolver(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @QueryMapping
    public List<Category> getCategories() {
        return categoryService.getAllCategories();
    }
}
