package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.dto.ListingUpdateInput;
import dev.marketplace.marketplace.enums.Condition;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.service.ListingService;
import dev.marketplace.marketplace.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class ListingMutationResolver {

    private final ListingService listingService;
    private final UserService userService;

    public ListingMutationResolver(ListingService listingService, UserService userService) {
        this.listingService = listingService;
        this.userService = userService;
    }

    @MutationMapping
    public Listing createListing(@Argument String title,
                                 @Argument Double price,
                                 @Argument String description,
                                 @Argument List<String> images,
                                 @Argument Condition condition,
                                 @Argument UUID categoryId,
                                 @Argument UUID userId,
                                 @Argument Integer quantity,
                                 @Argument UUID cityId,
                                 @Argument String customCity) {
        return listingService.createListing(title, description, images, categoryId, price, cityId, customCity, condition, userId, quantity);
    }

    @MutationMapping
    public boolean deleteListing(@Argument UUID listingId, @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userService.getUserIdByUsername(userDetails.getUsername());
        return listingService.deleteListing(listingId, userId);
    }

    @MutationMapping
    public Listing updateListing(@Argument ListingUpdateInput input,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userService.getUserIdByUsername(userDetails.getUsername());
        return listingService.updateListing(input, userId);
    }

    @MutationMapping
    public Listing markListingAsSold(@Argument UUID id,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userService.getUserIdByUsername(userDetails.getUsername());
        return listingService.markListingAsSold(id, userId);
    }


}
