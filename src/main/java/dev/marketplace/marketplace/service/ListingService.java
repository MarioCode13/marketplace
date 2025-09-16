package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.dto.ListingDTO;
import dev.marketplace.marketplace.dto.ListingPageResponse;
import dev.marketplace.marketplace.dto.ListingUpdateInput;
import dev.marketplace.marketplace.enums.Condition;
import dev.marketplace.marketplace.model.Business;
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
import dev.marketplace.marketplace.model.City;
import java.util.UUID;
import dev.marketplace.marketplace.exceptions.ListingLimitExceededException;
import dev.marketplace.marketplace.enums.BusinessType;

@Service
public class ListingService {
    private final ListingRepository listingRepository;
    private final ListingImageService imageService;
    private final ListingAuthorizationService authorizationService;
    private final CategoryService categoryService;
    private final CityService cityService;

    public ListingService(ListingRepository listingRepository,
                          ListingImageService imageService,
                          ListingAuthorizationService authorizationService,
                          CategoryService categoryService,
                          CityService cityService) {
        this.listingRepository = listingRepository;
        this.imageService = imageService;
        this.authorizationService = authorizationService;
        this.categoryService = categoryService;
        this.cityService = cityService;
    }

    public ListingPageResponse getListings(Integer limit, Integer offset, UUID categoryId, Double minPrice, Double maxPrice) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        List<Listing> listings;
        if (categoryId != null) {
            List<UUID> categoryIds = categoryService.getAllDescendantCategoryIds(categoryId);
            listings = listingRepository.findByCategoryIdIn(categoryIds);
        } else {
            Page<Listing> page = listingRepository.findAll(pageable);
            listings = page.getContent();
        }
        List<ListingDTO> listingDTOs = listings.stream().map(this::convertToDTO).toList();
        int total = listings.size();
        return new ListingPageResponse(listingDTOs, total);
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
                listing.getBusiness(),
                listing.getCreatedAt(),
                listing.isSold(),
                listing.getExpiresAt() != null ? listing.getExpiresAt().toString() : null,
                listing.isArchived() // Added
        );
    }

    public Optional<ListingDTO> getListingById(UUID id) {
        return listingRepository.findById(id).map(this::convertToDTO);
    }

    public Listing getListingByIdRaw(UUID id) {
        return listingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Listing not found with ID: " + id));
    }

    public List<ListingDTO> getListingsByCategory(UUID categoryId) {
        List<Listing> listings = listingRepository.findByCategoryId(categoryId);
        return listings.stream().map(this::convertToDTO).toList();
    }

    @Transactional
    public Listing createListing(String title, String description, List<String> imageUrls,
                                 UUID categoryId, double price, UUID cityId, String customCity, Condition condition, UUID userId) {
        imageService.validateImages(imageUrls);
        cityService.validateCityOrCustomCity(cityId, customCity);
        List<String> imageFilenames = imageService.convertUrlsToFilenames(imageUrls);
        User user = authorizationService.validateUserExists(userId);
        Category category = categoryService.findById(categoryId);
        City city = cityId != null ? cityService.getCityById(cityId) : null;
        Business business = authorizationService.getBusinessForUser(userId);
        int maxListings = Integer.MAX_VALUE;
        long currentListings = 0;
        if (business != null) {
            BusinessType businessType = business.getBusinessType();
            if (businessType == BusinessType.RESELLER) {
                maxListings = 20;
            } else if (businessType == BusinessType.PRO_STORE) {
                maxListings = Integer.MAX_VALUE;
            }
            currentListings = listingRepository.countByBusinessIdAndSoldFalseAndArchivedFalse(business.getId());
        } else {
            String planType = user.getPlanType();
            if ("FREE".equalsIgnoreCase(planType)) {
                maxListings = 3;
            } else if ("VERIFIED".equalsIgnoreCase(planType)) {
                maxListings = 10;
            } else if ("RESELLER".equalsIgnoreCase(planType)) {
                maxListings = 20;
            } else if ("PRO_STORE".equalsIgnoreCase(planType)) {
                maxListings = Integer.MAX_VALUE;
            }
            currentListings = listingRepository.countByUserIdAndSoldFalseAndArchivedFalse(user.getId());
        }
        if (currentListings >= maxListings) {
            throw new ListingLimitExceededException("Listing limit reached for your plan. Upgrade your plan to create more listings.");
        }
        Listing.Builder builder = new Listing.Builder()
                .title(title)
                .description(description)
                .images(imageFilenames)
                .category(category)
                .price(price)
                .city(city)
                .customCity(customCity)
                .condition(condition)
                .createdBy(user)
                .archived(false);
        if (business != null) {
            builder.business(business);
            builder.user(null);
        } else {
            builder.user(user);
            builder.business(null);
        }
        Listing listing = builder.build();
        return listingRepository.save(listing);
    }

    @Transactional
    public Listing updateListing(ListingUpdateInput input, UUID userId) {
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
        if (input.cityId() != null || input.customCity() != null) {
            cityService.validateCityOrCustomCity(input.cityId(), input.customCity());
            listing.setCity(input.cityId() != null ? cityService.getCityById(input.cityId()) : null);
            listing.setCustomCity(input.customCity());
        }
        if (input.categoryId() != null) {
            Category category = categoryService.findById(input.categoryId());
            if (category == null) {
                throw new RuntimeException("Category not found: " + input.categoryId());
            }
            listing.setCategory(category);
        }
        return listingRepository.save(listing);
    }

    @Transactional
    public boolean deleteListing(UUID listingId, UUID userId) {
        Listing listing = authorizationService.checkDeletePermission(listingId, userId);
        listingRepository.delete(listing);
        return true;
    }

    @Transactional
    public Listing sellListingToBuyer(UUID listingId, UUID sellerId, UUID buyerId,
                                     java.math.BigDecimal salePrice, String paymentMethod, String notes) {
        Listing listing = authorizationService.checkUpdatePermission(listingId, sellerId);
        // transactionService.createTransaction(listingId, buyerId, salePrice, paymentMethod, notes);
        return listing;
    }

    @Transactional
    public Listing save(Listing listing) {
        return listingRepository.save(listing);
    }

    /**
     * Returns listings with advanced filters and pagination.
     */
    public ListingPageResponse getListingsWithFilters(
            Integer limit,
            Integer offset,
            UUID categoryId,
            Double minPrice,
            Double maxPrice,
            Condition condition,
            UUID cityId,
            String searchTerm,
            java.time.LocalDateTime minDate,
            java.time.LocalDateTime maxDate,
            String sortBy,
            String sortOrder,
            UUID userId,
            UUID businessId // Added businessId argument
    ) {
        // Build Pageable
        Pageable pageable = (limit != null && offset != null) ? PageRequest.of(offset / limit, limit) : Pageable.unpaged();
        // If categoryId is provided, get all descendant category IDs (including itself)
        final List<UUID> categoryIds = (categoryId != null)
            ? categoryService.getAllDescendantCategoryIds(categoryId)
            : null;
        // Use repository filtering (pseudo-code, adapt to your repository's API)
        List<Listing> filteredListings = listingRepository.findAll();
        // Apply filters manually if repository does not support them
        filteredListings = filteredListings.stream()
            .filter(l -> categoryIds == null || (l.getCategory() != null && categoryIds.contains(l.getCategory().getId())))
            .filter(l -> minPrice == null || l.getPrice() >= minPrice)
            .filter(l -> maxPrice == null || l.getPrice() <= maxPrice)
            .filter(l -> condition == null || l.getCondition() == condition)
            .filter(l -> cityId == null || (l.getCity() != null && l.getCity().getId().equals(cityId)))
            .filter(l -> userId == null || (l.getUser() != null && l.getUser().getId().equals(userId)))
            .filter(l -> businessId == null || (l.getBusiness() != null && l.getBusiness().getId().equals(businessId)))
            .filter(l -> searchTerm == null || l.getTitle().toLowerCase().contains(searchTerm.toLowerCase()) || l.getDescription().toLowerCase().contains(searchTerm.toLowerCase()))
            .filter(l -> minDate == null || (l.getCreatedAt() != null && !l.getCreatedAt().isBefore(minDate)))
            .filter(l -> maxDate == null || (l.getCreatedAt() != null && !l.getCreatedAt().isAfter(maxDate)))
            .toList();
        // Sorting
        if (sortBy != null && sortOrder != null) {
            java.util.Comparator<Listing> comparator = null;
            if ("price".equalsIgnoreCase(sortBy)) {
                comparator = java.util.Comparator.comparing(Listing::getPrice);
            } else if ("createdAt".equalsIgnoreCase(sortBy)) {
                comparator = java.util.Comparator.comparing(Listing::getCreatedAt);
            }
            if (comparator != null) {
                if ("desc".equalsIgnoreCase(sortOrder)) {
                    comparator = comparator.reversed();
                }
                filteredListings = filteredListings.stream().sorted(comparator).toList();
            }
        }
        // Pagination
        int totalCount = filteredListings.size();
        List<Listing> paginatedListings = filteredListings;
        if (limit != null && offset != null) {
            int startIndex = offset;
            int endIndex = Math.min(startIndex + limit, totalCount);
            if (startIndex < totalCount) {
                paginatedListings = filteredListings.subList(startIndex, endIndex);
            } else {
                paginatedListings = List.of();
            }
        }
        List<ListingDTO> listingDTOs = paginatedListings.stream().map(this::convertToDTO).toList();
        return new ListingPageResponse(listingDTOs, totalCount);
    }

    /**
     * Returns all listings for a given user.
     */
    public List<ListingDTO> getListingsByUserId(UUID userId) {
        List<Listing> listings = listingRepository.findByUserId(userId);
        return listings.stream().map(this::convertToDTO).toList();
    }
}
