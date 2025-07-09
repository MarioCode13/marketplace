package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.dto.ListingDTO;
import dev.marketplace.marketplace.dto.ListingPageResponse;
import dev.marketplace.marketplace.enums.Condition;
import dev.marketplace.marketplace.model.Category;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.CategoryRepository;
import dev.marketplace.marketplace.repository.ListingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class ListingService {
    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;
    private final ListingImageService imageService;
    private final ListingValidationService validationService;
    private final ListingAuthorizationService authorizationService;

    public ListingService(ListingRepository listingRepository, 
                         CategoryRepository categoryRepository,
                         ListingImageService imageService,
                         ListingValidationService validationService,
                         ListingAuthorizationService authorizationService) {
        this.listingRepository = listingRepository;
        this.categoryRepository = categoryRepository;
        this.imageService = imageService;
        this.validationService = validationService;
        this.authorizationService = authorizationService;
    }

    public ListingPageResponse getListings(Integer limit, Integer offset, Long categoryId, Double minPrice, Double maxPrice) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Listing> page;

        if (categoryId != null || minPrice != null || maxPrice != null) {
            page = listingRepository.findFilteredListings(categoryId, minPrice, maxPrice, pageable);
        } else {
            page = listingRepository.findAll(pageable);
        }

        List<ListingDTO> listings = page.getContent().stream()
                .map(this::convertToDTO)
                .toList();

        return new ListingPageResponse(listings, (int) page.getTotalElements());
    }

    private ListingDTO convertToDTO(Listing listing) {
        List<String> preSignedUrls = imageService.generatePreSignedUrls(listing.getImages());

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
    }

    public Optional<ListingDTO> getListingById(Long id) {
        return listingRepository.findById(id)
                .map(this::convertToDTO);
    }

    public List<ListingDTO> getListingsByCategory(Long categoryId) {
        List<Listing> listings = listingRepository.findByCategoryId(categoryId);
        return listings.stream().map(this::convertToDTO).toList();
    }

    @Transactional
    public Listing createListing(String title, String description, List<String> imageFilenames,
                                 Long categoryId, double price, String location,
                                 Condition condition, Long userId) {

        validationService.validateListingCreation(title, description, price, location, condition, userId);
        imageService.validateImages(imageFilenames);

        User user = authorizationService.validateUserExists(userId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));

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

    public List<ListingDTO> getListingsByUserId(Long userId) {
        List<Listing> listings = listingRepository.findByUserId(userId);
        return listings.stream().map(this::convertToDTO).toList();
    }

    @Transactional
    private Listing updateListing(Long listingId, Long userId, Consumer<Listing> updateAction) {
        Listing listing = authorizationService.checkUpdatePermission(listingId, userId);
        
        updateAction.accept(listing);
        return listingRepository.save(listing);
    }

    @Transactional
    public boolean deleteListing(Long listingId, Long userId) {
        Listing listing = authorizationService.checkDeletePermission(listingId, userId);
        
        listingRepository.delete(listing);
        return true;
    }

    @Transactional
    public Listing updateListingPrice(Long listingId, Long userId, double newPrice) {
        validationService.validatePriceUpdate(newPrice);
        return updateListing(listingId, userId, listing -> listing.setPrice(newPrice));
    }

    @Transactional
    public Listing updateListingTitle(Long listingId, Long userId, String newTitle) {
        validationService.validateTitleUpdate(newTitle);
        return updateListing(listingId, userId, listing -> listing.setTitle(newTitle));
    }

    @Transactional
    public Listing updateListingDescription(Long listingId, Long userId, String newDescription) {
        validationService.validateDescriptionUpdate(newDescription);
        return updateListing(listingId, userId, listing -> listing.setDescription(newDescription));
    }

    @Transactional
    public Listing markListingAsSold(Long listingId, Long userId) {
        return updateListing(listingId, userId, listing -> listing.setSold(true));
    }

    public Listing save(Listing listing) {
        return listingRepository.save(listing);
    }
}
