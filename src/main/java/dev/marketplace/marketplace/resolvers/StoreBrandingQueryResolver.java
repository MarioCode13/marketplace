package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.repository.BusinessRepository;
import dev.marketplace.marketplace.repository.StoreBrandingRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class StoreBrandingQueryResolver {
    private final StoreBrandingRepository storeBrandingRepository;
    private final BusinessRepository businessRepository;

    public StoreBrandingQueryResolver(StoreBrandingRepository storeBrandingRepository, BusinessRepository businessRepository) {
        this.storeBrandingRepository = storeBrandingRepository;
        this.businessRepository = businessRepository;
    }

    @QueryMapping
    public Boolean isStoreSlugAvailable(@Argument String slug) {
        return businessRepository.findBySlug(slug).isEmpty();
    }
}
