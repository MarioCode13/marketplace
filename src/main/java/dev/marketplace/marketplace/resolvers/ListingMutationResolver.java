package dev.marketplace.marketplace.resolvers;
import com.backblaze.b2.client.exceptions.B2Exception;
import dev.marketplace.marketplace.enums.Condition;
import dev.marketplace.marketplace.model.Category;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.CategoryRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import dev.marketplace.marketplace.service.ListingService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ListingMutationResolver {

    private final ListingService listingService;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public ListingMutationResolver(ListingService listingService, CategoryRepository categoryRepository, UserRepository userRepository) {
        this.listingService = listingService;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @MutationMapping
    public Listing createListing(@Argument String title,
                                 @Argument Double price,
                                 @Argument String description,
                                 @Argument String location,
                                 @Argument List<String> images,
                                 @Argument Condition condition,
                                 @Argument Long categoryId,
                                 @Argument Long userId) {
        // Fetch category and user
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Create the Listing using the builder
        Listing listing = new Listing.Builder()
                .title(title)
                .price(price)
                .description(description)
                .location(location)
                .images(images)
                .condition(condition)
                .category(category)
                .user(user)
                .build();

        return listingService.save(listing);
    }



}
