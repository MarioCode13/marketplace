package dev.marketplace.marketplace.service;

import com.backblaze.b2.client.exceptions.B2Exception;
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
import java.util.Objects;
import java.util.Optional;

@Service
public class ListingService {
    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final B2StorageService b2StorageService;

    public ListingService(ListingRepository listingRepository, CategoryRepository categoryRepository, UserRepository userRepository, B2StorageService b2StorageService) {
        this.listingRepository = listingRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.b2StorageService = b2StorageService;
    }

    public List<Listing> getAllListings() {
        List<Listing> listings = listingRepository.findAll();

        return listings.stream().map(listing -> {
            List<String> preSignedUrls = listing.getImages().stream()
                    .map(fileName -> {
                        try {
                            return b2StorageService.generatePreSignedUrl(fileName);
                        } catch (B2Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            return new Listing.Builder() // ✅ Use "new Listing.Builder()"
                    .id(listing.getId())
                    .title(listing.getTitle())
                    .description(listing.getDescription())
                    .images(preSignedUrls) // ✅ Pre-signed URLs
                    .category(listing.getCategory())
                    .price(listing.getPrice())
                    .location(listing.getLocation())
                    .condition(listing.getCondition())
                    .user(listing.getUser())
                    .build();
        }).toList();
    }


    public Optional<Listing> getListingById(String id) {
        return listingRepository.findById(Long.parseLong(id));
    }

    public List<Listing> getListingsByCategory(String categoryId) {
        return listingRepository.findByCategoryId(Long.parseLong(categoryId));
    }

    public Listing createListing(String title, String description, List<String> imageFilenames,
                                 String categoryId, double price, String location,
                                 Condition condition, String userId) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        Category category = categoryRepository.findById(Long.parseLong(categoryId))
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Listing listing = new Listing.Builder() // ✅ Use manual builder
                .title(title)
                .description(description)
                .images(imageFilenames)
                .category(category)
                .price(price)
                .location(location)
                .condition(condition)
                .user(user)
                .build();

        return listingRepository.save(listing);
    }


    public void deleteListing(String id) {
        listingRepository.deleteById(Long.parseLong(id));
    }

    public Listing save(Listing listing) {
        return listingRepository.save(listing);
    }
}
