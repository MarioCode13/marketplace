package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.dto.ListingDTO;
import dev.marketplace.marketplace.dto.ListingPageResponse;
import dev.marketplace.marketplace.dto.ListingUpdateInput;
import dev.marketplace.marketplace.enums.Condition;
import dev.marketplace.marketplace.model.*;
import dev.marketplace.marketplace.mapper.UserMapper;
import dev.marketplace.marketplace.repository.ListingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import dev.marketplace.marketplace.exceptions.ListingLimitExceededException;

@Service
public class ListingService {
    private final ListingImageService imageService;
    private final ListingAuthorizationService authorizationService;
    private final ListingRepository listingRepository;
    private final CategoryService categoryService;
    private final CityService cityService;
    private final SubscriptionService subscriptionService;
    private final dev.marketplace.marketplace.repository.BusinessRepository businessRepository;

    public ListingService(ListingRepository listingRepository,
                          ListingImageService imageService,
                          ListingAuthorizationService authorizationService,
                          CategoryService categoryService,
                          CityService cityService,
                          SubscriptionService subscriptionService,
                          dev.marketplace.marketplace.repository.BusinessRepository businessRepository) {
        this.listingRepository = listingRepository;
        this.imageService = imageService;
        this.authorizationService = authorizationService;
        this.categoryService = categoryService;
        this.cityService = cityService;
        this.subscriptionService = subscriptionService;
        this.businessRepository = businessRepository;
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
                listing.getQuantity(),
                listing.getCity(),
                listing.getCustomCity(),
                listing.getCondition().name(),
                UserMapper.toDto(listing.getUser()),
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
                                 UUID categoryId, double price, UUID cityId, String customCity, Condition condition, UUID userId, Integer quantity, UUID businessId) {
        imageService.validateImages(imageUrls);
        cityService.validateCityOrCustomCity(cityId, customCity);
        List<String> imageFilenames = imageService.convertUrlsToFilenames(imageUrls);
        User user = authorizationService.validateUserExists(userId);
        Category category = categoryService.findById(categoryId);
        City city = cityId != null ? cityService.getCityById(cityId) : null;
        
        Business business = null;
        if (businessId != null) {
            // Validate business exists
            business = businessRepository.findById(businessId)
                    .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));
            
            // Validate user has permission to create listings for this business
            if (!business.canUserCreateListings(user)) {
                throw new RuntimeException("User does not have permission to create listings for this business");
            }
        } else {
            // Personal listing - enforce limit
            int maxListings = 10; // This should be dynamic based on plan, but kept simple here
            long currentListings = listingRepository.countByUserIdAndSoldFalseAndArchivedFalse(userId);
            if (currentListings >= maxListings) {
                throw new ListingLimitExceededException("Listing limit reached for your plan. Upgrade your plan to create more listings.");
            }
        }
        
        // Determine allowed quantity based on subscription plan
        int listingQuantity = 1; // default
        boolean canSpecifyQuantity = false;
        try {
            var activeSub = subscriptionService.getActiveSubscription(userId);
            if (activeSub.isPresent()) {
                var plan = activeSub.get().getPlanType();
                if (plan == dev.marketplace.marketplace.model.Subscription.PlanType.RESELLER || plan == dev.marketplace.marketplace.model.Subscription.PlanType.PRO_STORE) {
                    canSpecifyQuantity = true;
                }
            }
        } catch (Exception ignored) {
            // if subscription service fails, fall back to default
        }

        if (quantity != null && canSpecifyQuantity) {
            listingQuantity = Math.max(0, quantity);
        }

        Listing.Builder listingBuilder = new Listing.Builder()
              .title(title)
              .description(description)
              .images(imageFilenames)
              .category(category)
              .price(price)
              .quantity(listingQuantity)
              .city(city)
              .customCity(customCity)
              .condition(condition)
              .archived(false)
              .sold(false)
              .createdAt(java.time.LocalDateTime.now());
        
        // Set ownership fields based on whether this is a business or personal listing
        if (business != null) {
            // Business listing: set business, user (business owner), and createdBy (actual creator)
            listingBuilder
                .business(business)
                .user(business.getOwner())  // Business owner for the listing
                .createdBy(user);  // Actual creator for audit
        } else {
            // Personal listing: user creates for themselves
            listingBuilder
                .user(user)
                .createdBy(user);
        }
        
        Listing listing = listingBuilder.build();
        return listingRepository.save(listing);
    }

    @Transactional
    public Listing updateListing(ListingUpdateInput input, UUID userId) {
        // Use the authorization service to validate the caller has permission to update the listing.
        // This handles both personal listings (listing.user) and business listings (listing.business)
        Listing listing = authorizationService.checkUpdatePermission(input.id(), userId);
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
        // Handle quantity update: only allowed for RESELLER and PRO_STORE plans
        if (input.quantity() != null) {
            boolean allowed = false;
            try {
                var activeSub = subscriptionService.getActiveSubscription(userId);
                if (activeSub.isPresent()) {
                    var plan = activeSub.get().getPlanType();
                    if (plan == dev.marketplace.marketplace.model.Subscription.PlanType.RESELLER
                            || plan == dev.marketplace.marketplace.model.Subscription.PlanType.PRO_STORE) {
                        allowed = true;
                    }
                }
            } catch (Exception ignored) {}
            if (allowed) {
                int newQty = Math.max(0, input.quantity());
                listing.setQuantity(newQty);
            }
            // if not allowed, silently ignore client-supplied quantity
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
            UUID businessId
    ) {
        Pageable pageable = (limit != null && offset != null) ? PageRequest.of(offset / limit, limit) : Pageable.unpaged();

        final List<UUID> categoryIds = (categoryId != null)
            ? categoryService.getAllDescendantCategoryIds(categoryId)
            : null;

        List<Listing> filteredListings = listingRepository.findAll();

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

    @Transactional
    public Listing markListingAsSold(UUID listingId, UUID userId) {
        // Load with pessimistic lock to avoid concurrent oversells
        Listing listing = listingRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found with ID: " + listingId));

        // Authorization: allow if user is owner or is associated with the listing's business
        boolean allowed = false;
        if (listing.getUser() != null && listing.getUser().getId().equals(userId)) {
            allowed = true;
        } else if (listing.getBusiness() != null) {
            try {
                dev.marketplace.marketplace.model.Business userBusiness = authorizationService.getBusinessForUser(userId);
                if (userBusiness != null && listing.getBusiness() != null && userBusiness.getId().equals(listing.getBusiness().getId())) {
                    allowed = true;
                }
            } catch (Exception ignored) {}
        }
        if (!allowed) {
            throw new RuntimeException("Unauthorized to mark this listing as sold");
        }

        // Business logic: prevent selling when out of stock
        int qty = listing.getQuantity();
        if (qty <= 0) {
            throw new IllegalArgumentException("Listing is out of stock");
        }

        // decrement quantity or mark sold/archived when reaching zero
        if (qty > 1) {
            listing.setQuantity(qty - 1);
        } else {
            listing.setQuantity(0);
            listing.setSold(true);
            listing.setArchived(true);
            listing.setSoldAt(java.time.LocalDateTime.now());
        }

        return listingRepository.save(listing);
    }
}
