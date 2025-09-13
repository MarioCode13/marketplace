package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.repository.StoreBrandingRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class StoreBrandingQueryResolver {
    private final StoreBrandingRepository storeBrandingRepository;

    public StoreBrandingQueryResolver(StoreBrandingRepository storeBrandingRepository) {
        this.storeBrandingRepository = storeBrandingRepository;
    }

    @QueryMapping
    public Boolean isStoreSlugAvailable(@Argument String slug) {
        return storeBrandingRepository.findBySlug(slug).isEmpty();
    }
}

