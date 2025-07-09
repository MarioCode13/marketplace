package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.dto.ListingDTO;
import dev.marketplace.marketplace.dto.ListingPageResponse;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.service.ListingService;
import dev.marketplace.marketplace.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:3000")
@Controller
public class ListingQueryResolver {
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

    @QueryMapping
    public ListingDTO getListingById(@Argument Long id) {
        return listingService.getListingById(id)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found with ID: " + id));
    }

    @QueryMapping
    public List<ListingDTO> getListingsByCategory(@Argument Long categoryId) {
        return listingService.getListingsByCategory(categoryId);
    }

    @QueryMapping
    public List<ListingDTO> myListings(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return listingService.getListingsByUserId(user.getId());
    }
}
