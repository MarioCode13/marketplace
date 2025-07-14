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
import org.springframework.graphql.data.method.annotation.SchemaMapping;
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
            @Argument String categoryId,
            @Argument Double minPrice,
            @Argument Double maxPrice,
            @Argument String condition,
            @Argument String location,
            @Argument String searchTerm,
            @Argument String minDate,
            @Argument String maxDate,
            @Argument String sortBy,
            @Argument String sortOrder
    ) {
        // Convert condition string to enum if provided
        dev.marketplace.marketplace.enums.Condition conditionEnum = null;
        if (condition != null && !condition.isEmpty()) {
            try {
                conditionEnum = dev.marketplace.marketplace.enums.Condition.valueOf(condition.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid condition, ignore it
            }
        }

        // Convert date strings to LocalDateTime if provided
        java.time.LocalDateTime minDateTime = null;
        java.time.LocalDateTime maxDateTime = null;
        
        if (minDate != null && !minDate.isEmpty()) {
            try {
                // Parse as LocalDate first (YYYY-MM-DD format from HTML date input)
                java.time.LocalDate minDateParsed = java.time.LocalDate.parse(minDate);
                minDateTime = minDateParsed.atStartOfDay();
            } catch (Exception e) {
                // Invalid date format, ignore it
                System.err.println("Failed to parse minDate: " + minDate + ", error: " + e.getMessage());
            }
        }
        
        if (maxDate != null && !maxDate.isEmpty()) {
            try {
                // Parse as LocalDate first (YYYY-MM-DD format from HTML date input)
                java.time.LocalDate maxDateParsed = java.time.LocalDate.parse(maxDate);
                maxDateTime = maxDateParsed.atTime(23, 59, 59); // End of day
            } catch (Exception e) {
                // Invalid date format, ignore it
                System.err.println("Failed to parse maxDate: " + maxDate + ", error: " + e.getMessage());
            }
        }

        // Convert categoryId string to Long if provided
        Long categoryIdLong = null;
        if (categoryId != null && !categoryId.isEmpty()) {
            try {
                categoryIdLong = Long.parseLong(categoryId);
            } catch (NumberFormatException e) {
                // Invalid category ID, ignore it
                System.err.println("Failed to parse categoryId: " + categoryId + ", error: " + e.getMessage());
            }
        }

        return listingService.getListingsWithFilters(
            limit, offset, categoryIdLong, minPrice, maxPrice, 
            conditionEnum, location, searchTerm, minDateTime, maxDateTime, sortBy, sortOrder);
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
    public ListingPageResponse myListings(
            @AuthenticationPrincipal UserDetails userDetails,
            @Argument Integer limit,
            @Argument Integer offset
    ) {
        User user = userService.getUserByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<ListingDTO> allListings = listingService.getListingsByUserId(user.getId());
        int totalCount = allListings.size();
        
        // Apply pagination if parameters are provided
        if (limit != null && offset != null) {
            int startIndex = offset;
            int endIndex = Math.min(startIndex + limit, allListings.size());
            
            if (startIndex >= allListings.size()) {
                return new ListingPageResponse(List.of(), totalCount); // Return empty list if offset is beyond available items
            }
            
            List<ListingDTO> paginatedListings = allListings.subList(startIndex, endIndex);
            return new ListingPageResponse(paginatedListings, totalCount);
        }
        
        return new ListingPageResponse(allListings, totalCount);
    }

    @QueryMapping
    public List<ListingDTO> listingsByUser(@Argument Long userId) {
        return listingService.getListingsByUserId(userId);
    }

}
