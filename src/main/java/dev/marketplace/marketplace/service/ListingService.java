package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.enums.Condition;
import dev.marketplace.marketplace.model.Category;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.CategoryRepository;
import dev.marketplace.marketplace.repository.ListingRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ListingService {
    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public ListingService(ListingRepository listingRepository, CategoryRepository categoryRepository, UserRepository userRepository) {
        this.listingRepository = listingRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public List<Listing> getAllListings(){
        return listingRepository.findAll();
    }

    public Optional<Listing> getListingById(Long id) {
        return listingRepository.findById(id);
    }
    public List<Listing> getListingsByCategory(Long categoryId) {
        return listingRepository.findByCategoryId(categoryId);
    }


    public Listing createListing(String title, String description, List<String> imageUrls,
                                 Long categoryId, double price, String location,
                                 Condition condition, Long userId) {
        // Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch category
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Build listing
        Listing listing = Listing.builder()
                .title(title)
                .description(description)
                .images(imageUrls)
                .category(category)
                .price(price)
                .location(location)
                .condition(condition)
                .user(user)
                .build();

        return listingRepository.save(listing);
    }


//    public Listing updateListing(Long id, String title, String description, List<String> imageUrls,
//                                 Long categoryId, double price, String location,
//                                 Condition condition, boolean sold) {
//        return listingRepository.findById(id)
//                .map(listing -> {
//                    listing.setTitle(title);
//                    listing.setDescription(description);
//                    listing.setPrice(price);
//                    listing.setLocation(location);
//                    listing.setCondition(condition);
//                    listing.setSold(sold);
//
//                    // Update category if provided
//                    if (categoryId != null) {
//                        Category category = categoryRepository.findById(categoryId)
//                                .orElseThrow(() -> new RuntimeException("Category not found"));
//                        listing.setCategory(category);
//                    }
//
//                    // Update images if provided
//                    if (imageUrls != null && !imageUrls.isEmpty()) {
//                        listing.setImages(imageUrls);
//                    }
//
//                    return listingRepository.save(listing);
//                }).orElseThrow(() -> new RuntimeException("Listing not found"));
//    }


    public void deleteListing(Long id) {
        listingRepository.deleteById(id);
    }
}
