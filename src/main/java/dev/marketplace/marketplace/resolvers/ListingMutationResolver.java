package dev.marketplace.marketplace.resolvers;
import com.backblaze.b2.client.exceptions.B2Exception;
import dev.marketplace.marketplace.enums.Condition;
import dev.marketplace.marketplace.model.Category;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.CategoryRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import dev.marketplace.marketplace.service.ListingService;
import dev.marketplace.marketplace.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ListingMutationResolver {

    private final ListingService listingService;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public ListingMutationResolver(ListingService listingService, CategoryRepository categoryRepository, UserRepository userRepository, UserService userService) {
        this.listingService = listingService;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.userService = userService;
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

    @MutationMapping
    public boolean deleteListing(@Argument Long listingId, @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return listingService.deleteListing(listingId, userId);
    }

    @MutationMapping
    public Listing updateListingPrice(@Argument Long listingId, 
                                     @Argument Double newPrice, 
                                     @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return listingService.updateListingPrice(listingId, userId, newPrice);
    }

    @MutationMapping
    public Listing updateListingTitle(@Argument Long listingId, 
                                     @Argument String newTitle, 
                                     @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return listingService.updateListingTitle(listingId, userId, newTitle);
    }

    @MutationMapping
    public Listing updateListingDescription(@Argument Long listingId, 
                                           @Argument String newDescription, 
                                           @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return listingService.updateListingDescription(listingId, userId, newDescription);
    }

    @MutationMapping
    public Listing markListingAsSold(@Argument Long listingId, 
                                    @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return listingService.markListingAsSold(listingId, userId);
    }


}
