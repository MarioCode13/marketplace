package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.dto.ListingDTO;
import dev.marketplace.marketplace.dto.ListingPageResponse;
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
import java.util.UUID;

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
            @Argument UUID categoryId,
            @Argument String categorySlug,
            @Argument Double minPrice,
            @Argument Double maxPrice,
            @Argument String condition,
            @Argument UUID cityId,
            @Argument String citySlug,
            @Argument String searchTerm,
            @Argument String minDate,
            @Argument String maxDate,
            @Argument String sortBy,
            @Argument String sortOrder,
            @Argument UUID userId,
            @Argument UUID businessId // Added businessId argument
    ) {
        // Resolve slug inputs to IDs if provided
        // Only resolve slug if categoryId is not already provided (to avoid conflicts)
        if (categoryId == null && categorySlug != null && !categorySlug.isBlank()) {
            try {
                categoryId = listingService.getCategoryService().findBySlug(categorySlug).getId();
            } catch (Exception e) {
                // If slug lookup fails, categoryId remains null and query will return all categories
                throw new RuntimeException("Category not found with slug: " + categorySlug, e);
            }
        }
        // Only resolve city slug if cityId is not already provided
        if (cityId == null && citySlug != null && !citySlug.isBlank()) {
            try {
                cityId = listingService.getCityService().getCityBySlug(citySlug).getId();
            } catch (Exception e) {
                // If slug lookup fails, cityId remains null
                throw new RuntimeException("City not found with slug: " + citySlug, e);
            }
        }
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

        return listingService.getListingsWithFilters(
            limit,
            offset,
            categoryId,
            minPrice,
            maxPrice,
            conditionEnum,
            cityId,
            searchTerm,
            minDateTime,
            maxDateTime,
            sortBy,
            sortOrder,
            userId,
            businessId // Pass businessId to service
        );
    }

    @QueryMapping
    public ListingDTO getListingById(@Argument UUID id) {
        return listingService.getListingById(id)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found with ID: " + id));
    }

    @QueryMapping
    public List<ListingDTO> getListingsByCategory(@Argument UUID categoryId) {
        return listingService.getListingsByCategory(categoryId);
    }

    @QueryMapping
    public ListingPageResponse myListings(
            @AuthenticationPrincipal UserDetails userDetails,
            @Argument Integer limit,
            @Argument Integer offset
    ) {
        // Use the UserService method that returns the user's UUID to avoid loading the User entity here
        UUID userId = userService.getUserIdByUsername(userDetails.getUsername());
        List<ListingDTO> allListings = listingService.getListingsByUserId(userId);
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
    public List<ListingDTO> listingsByUser(@Argument UUID userId) {
        return listingService.getListingsByUserId(userId);
    }

}
