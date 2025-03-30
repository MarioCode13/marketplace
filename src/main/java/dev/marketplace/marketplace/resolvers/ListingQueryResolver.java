package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.dto.ListingDTO;
import dev.marketplace.marketplace.dto.ListingPageResponse;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.service.ListingService;
import dev.marketplace.marketplace.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:3000")
@Controller
public class ListingQueryResolver  {
    private final ListingService listingService;
    private final UserService userService;

    public ListingQueryResolver(ListingService listingService, UserService userService) {
        this.listingService = listingService;
        this.userService = userService;
    }


    @QueryMapping
    public ListingPageResponse getListings(
            @Argument Integer limit,
            @Argument Integer offset,
            @Argument Long categoryId,
            @Argument Double minPrice,
            @Argument Double maxPrice
    ) {
        return listingService.getListings(limit, offset, categoryId, minPrice, maxPrice);
    }

//    @QueryMapping
//    public ListingDTO getListingById(@Argument String id) {
//        return listingService.getListingById(id)
//                .orElseThrow(() -> new RuntimeException("Listing not found"));
//    }
    @QueryMapping
    public ListingDTO getListingById(@Argument Long id) {
        return listingService.getListingById(id)
                .map(listing -> new ListingDTO(
                        listing.id(),
                        listing.title(),
                        listing.description(),
                        listing.images(),
                        listing.category(),
                        listing.price(),
                        listing.location(),
                        listing.condition(), // Convert enum to String
                        listing.user(),
                        listing.createdAt(),
                        listing.sold(),
                        listing.expiresAt()
                ))
                .orElseThrow(() -> new RuntimeException("Listing not found"));
    }


    @QueryMapping
    public List<Listing> getListingsByCategory(@Argument Integer categoryId) { // ✅ Use Integer for GraphQL Int!
        return listingService.getListingsByCategory(categoryId.toString()); // Convert to Long
    }

    @QueryMapping
    public List<ListingDTO> myListings(@AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> user = userService.getUserByEmail(userDetails.getUsername());

        // Ensure the user exists
        User existingUser = user.orElseThrow(() -> new RuntimeException("User not found"));

        return listingService.getListingsByUserId(existingUser.getId());
    }



}
