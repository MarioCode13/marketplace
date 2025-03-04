package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.service.ListingService;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:3000")
@Controller
public class ListingQueryResolver  {
    private final ListingService listingService;

    public ListingQueryResolver(ListingService listingService) {
        this.listingService = listingService;
    }
    @QueryMapping
    public List<Listing> getAllListings() {
        return listingService.getAllListings();
    }
    @QueryMapping
    public Optional<Listing> getListingById(Long id) {
        return listingService.getListingById(id);
    }
    @QueryMapping
    public List<Listing> getListingsByCategory(Long categoryId) {
        return listingService.getListingsByCategory(categoryId);
    }
}
