package dev.marketplace.marketplace.service;

import com.backblaze.b2.client.exceptions.B2Exception;
import dev.marketplace.marketplace.dto.ListingDTO;
import dev.marketplace.marketplace.dto.ListingPageResponse;
import dev.marketplace.marketplace.enums.Condition;
import dev.marketplace.marketplace.model.Category;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.CategoryRepository;
import dev.marketplace.marketplace.repository.ListingRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    public ListingPageResponse getListings(Integer limit, Integer offset, Long categoryId, Double minPrice, Double maxPrice) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Listing> page;

        if (categoryId != null || minPrice != null || maxPrice != null) {
            page = listingRepository.findFilteredListings(categoryId, minPrice, maxPrice, pageable);
        } else {
            page = listingRepository.findAll(pageable);
        }

        List<ListingDTO> listings = page.getContent().stream().map(listing -> {
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

            return new ListingDTO(
                    listing.getId(),
                    listing.getTitle(),
                    listing.getDescription(),
                    preSignedUrls,
                    listing.getCategory(),
                    listing.getPrice(),
                    listing.getLocation(),
                    listing.getCondition().name(),
                    listing.getUser(),
                    listing.getCreatedAt(),
                    listing.isSold(),  // ✅ Ensure `sold` is included
                    listing.getExpiresAt().toString() // ✅ Ensure `expiresAt` is included
            );
        }).toList();

        return new ListingPageResponse(listings, (int) page.getTotalElements());
    }


    public Optional<ListingDTO> getListingById(String id) {
        return listingRepository.findById(Long.parseLong(id))
                .map(listing -> {
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

                    return new ListingDTO(
                            listing.getId(),
                            listing.getTitle(),
                            listing.getDescription(),
                            preSignedUrls,
                            listing.getCategory(),
                            listing.getPrice(),
                            listing.getLocation(),
                            listing.getCondition().name(),
                            listing.getUser(),
                            listing.getCreatedAt(),
                            listing.isSold(),
                            listing.getExpiresAt().toString()
                    );
                });
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

        Listing listing = new Listing.Builder()
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
