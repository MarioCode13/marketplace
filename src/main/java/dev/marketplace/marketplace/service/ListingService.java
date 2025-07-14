package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.dto.ListingDTO;
import dev.marketplace.marketplace.dto.ListingPageResponse;
import dev.marketplace.marketplace.enums.Condition;
import dev.marketplace.marketplace.model.Category;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.CategoryRepository;
import dev.marketplace.marketplace.repository.ListingRepository;
import dev.marketplace.marketplace.service.TransactionService;
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
    private final TransactionService transactionService;

    public ListingService(ListingRepository listingRepository, 
                         CategoryRepository categoryRepository,
                         ListingImageService imageService,
                         ListingValidationService validationService,
                         ListingAuthorizationService authorizationService,
                         TransactionService transactionService) {
        this.listingRepository = listingRepository;
        this.categoryRepository = categoryRepository;
        this.imageService = imageService;
        this.validationService = validationService;
        this.authorizationService = authorizationService;
        this.transactionService = transactionService;
    }

    public ListingPageResponse getListings(Integer limit, Integer offset, Long categoryId, Double minPrice, Double maxPrice) {
        return getListingsWithFilters(limit, offset, categoryId, minPrice, maxPrice, null, null, null, null, null, null, null);
    }

    public ListingPageResponse getListingsWithFilters(Integer limit, Integer offset, 
                                                    Long categoryId, Double minPrice, Double maxPrice,
                                                    Condition condition, String location, String searchTerm,
                                                    java.time.LocalDateTime minDate, java.time.LocalDateTime maxDate,
                                                    String sortBy, String sortOrder) {
        // Create pageable with sorting
        Pageable pageable;
        if (sortBy != null && !sortBy.isEmpty()) {
            org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(
                "desc".equalsIgnoreCase(sortOrder) ? 
                org.springframework.data.domain.Sort.Direction.DESC : 
                org.springframework.data.domain.Sort.Direction.ASC,
                sortBy
            );
            pageable = PageRequest.of(offset / limit, limit, sort);
        } else {
            pageable = PageRequest.of(offset / limit, limit);
        }
        
        Page<Listing> page;

        // Use enhanced filtering if any new filters are provided
        if (categoryId != null || minPrice != null || maxPrice != null || 
            condition != null || location != null || searchTerm != null || 
            minDate != null || maxDate != null) {
            page = listingRepository.findFilteredListings(
                categoryId, minPrice, maxPrice, condition, location, 
                searchTerm, pageable);
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

    public Listing getListingByIdRaw(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found with ID: " + id));
    }

    public List<ListingDTO> getListingsByCategory(Long categoryId) {
        List<Listing> listings = listingRepository.findByCategoryId(categoryId);
        return listings.stream().map(this::convertToDTO).toList();
    }

    @Transactional
    public Listing createListing(String title, String description, List<String> imageUrls,
                                 Long categoryId, double price, String location,
                                 Condition condition, Long userId) {

        validationService.validateListingCreation(title, description, price, location, condition, userId);
        imageService.validateImages(imageUrls);

        // Convert URLs to filenames for database storage
        List<String> imageFilenames = imageService.convertUrlsToFilenames(imageUrls);

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
        // This method is deprecated - use createTransaction instead
        // Keeping for backward compatibility but it should not be used
        return updateListing(listingId, userId, listing -> listing.setSold(true));
    }
    
    /**
     * Mark a listing as sold to a specific buyer (creates a transaction)
     */
    @Transactional
    public Listing sellListingToBuyer(Long listingId, Long sellerId, Long buyerId, 
                                     java.math.BigDecimal salePrice, String paymentMethod, String notes) {
        // Validate seller owns the listing
        Listing listing = authorizationService.checkUpdatePermission(listingId, sellerId);
        
        // Create transaction
        transactionService.createTransaction(listingId, buyerId, salePrice, paymentMethod, notes);
        
        return listing;
    }

    public Listing save(Listing listing) {
        return listingRepository.save(listing);
    }
}
