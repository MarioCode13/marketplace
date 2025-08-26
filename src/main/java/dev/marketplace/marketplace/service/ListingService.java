package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.dto.ListingDTO;
import dev.marketplace.marketplace.dto.ListingPageResponse;
import dev.marketplace.marketplace.dto.ListingUpdateInput;
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
import dev.marketplace.marketplace.model.City;
import dev.marketplace.marketplace.repository.CityRepository;

@Service
public class ListingService {
    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;
    private final ListingImageService imageService;
    private final ListingValidationService validationService;
    private final ListingAuthorizationService authorizationService;
    private final TransactionService transactionService;
    private final CityRepository cityRepository;
    private final CategoryService categoryService;
    private final CityService cityService;

    public ListingService(ListingRepository listingRepository,
                          CategoryRepository categoryRepository,
                          ListingImageService imageService,
                          ListingValidationService validationService,
                          ListingAuthorizationService authorizationService,
                          TransactionService transactionService,
                          CityRepository cityRepository, CategoryService categoryService, CityService cityService) {
        this.listingRepository = listingRepository;
        this.categoryRepository = categoryRepository;
        this.imageService = imageService;
        this.validationService = validationService;
        this.authorizationService = authorizationService;
        this.transactionService = transactionService;
        this.cityRepository = cityRepository;
        this.categoryService = categoryService;
        this.cityService = cityService;
    }

    public ListingPageResponse getListings(Integer limit, Integer offset, Long categoryId, Double minPrice, Double maxPrice) {
        return getListingsWithFilters(limit, offset, categoryId, minPrice, maxPrice, null, null, null, null, null, null, null, null);
    }

    public ListingPageResponse getListingsWithFilters(Integer limit, Integer offset, 
                                                    Long categoryId, Double minPrice, Double maxPrice,
                                                    Condition condition, Long cityId, String searchTerm,
                                                    java.time.LocalDateTime minDate, java.time.LocalDateTime maxDate,
                                                    String sortBy, String sortOrder, Long userId) {
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

        // Ensure searchTerm is never null for LIKE queries
        if (searchTerm == null) {
            searchTerm = "";
        }
        // Use enhanced filtering if any new filters are provided
        if (categoryId != null || minPrice != null || maxPrice != null || 
            condition != null || cityId != null || searchTerm != null || 
            minDate != null || maxDate != null || userId != null) {
            page = listingRepository.findFilteredListings(
                categoryId, minPrice, maxPrice, condition, cityId, 
                searchTerm, userId, pageable);
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
                listing.getCity(),
                listing.getCustomCity(),
                listing.getCondition().name(),
                listing.getUser(),
                listing.getCreatedAt(),
                listing.isSold(),
                listing.getExpiresAt() != null ? listing.getExpiresAt().toString() : null
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
                                 Long categoryId, double price, Long cityId, String customCity,
                                 Condition condition, Long userId) {

        validationService.validateListingCreation(title, description, price, cityId, condition, userId, customCity);
        imageService.validateImages(imageUrls);

        cityService.validateCityOrCustomCity(cityId, customCity);

        List<String> imageFilenames = imageService.convertUrlsToFilenames(imageUrls);

        User user = authorizationService.validateUserExists(userId);
        Category category = categoryService.findById(categoryId);

        City city = cityId != null ? cityService.getCityById(cityId) : null;

        Listing listing = new Listing.Builder()
                .title(title)
                .description(description)
                .images(imageFilenames)
                .category(category)
                .price(price)
                .city(city)
                .customCity(customCity)
                .condition(condition)
                .user(user)
                .build();

        return listingRepository.save(listing);
    }

    public List<ListingDTO> getListingsByUserId(Long userId) {
        List<Listing> listings = listingRepository.findByUserId(userId);
        return listings.stream().map(this::convertToDTO).toList();
    }



    public Listing updateListing(ListingUpdateInput input, Long userId) {
        Listing listing = listingRepository.findById(input.id())
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        if (!listing.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized update attempt");
        }

        if (input.title() != null) listing.setTitle(input.title());
        if (input.price() != null) listing.setPrice(input.price());
        if (input.description() != null) listing.setDescription(input.description());
        if (input.images() != null) listing.setImages(input.images());
        if (input.condition() != null) listing.setCondition(input.condition());

        // Validate city/customCity rules
        if (input.cityId() != null || input.customCity() != null) {
            cityService.validateCityOrCustomCity(input.cityId(), input.customCity());
            listing.setCity(input.cityId() != null ? cityService.getCityById(input.cityId()) : null);
            listing.setCustomCity(input.customCity());
        }


        if (input.categoryId() != null) {
            try {
                Category category = categoryService.findById(input.categoryId());
                if (category == null) {
                    throw new RuntimeException("Category not found: " + input.categoryId());
                }
                listing.setCategory(category);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid category ID: " + input.categoryId());
            }
        }

        return listingRepository.save(listing);
    }

    @Transactional
    public boolean deleteListing(Long listingId, Long userId) {
        Listing listing = authorizationService.checkDeletePermission(listingId, userId);
        
        listingRepository.delete(listing);
        return true;
    }



//    @Transactional
//    public Listing markListingAsSold(Long listingId, Long userId) {
//        // This method is deprecated - use createTransaction instead
//        // Keeping for backward compatibility but it should not be used
//        return updateListing(listingId, userId, listing -> listing.setSold(true));
//    }
    
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

    public void save(Listing listing) {}

}
