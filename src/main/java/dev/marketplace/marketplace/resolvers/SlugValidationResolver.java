package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.service.SlugValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class SlugValidationResolver {

    private final SlugValidationService slugValidationService;

    /**
     * Validate if a slug is available.
     *
     * @param slug The slug to validate
     * @param businessId Optional business ID (when updating, excludes this business from duplicate check)
     * @return SlugValidationResult with status and details
     */
    @QueryMapping
    public SlugValidationService.SlugValidationResult validateSlug(
            @Argument String slug,
            @Argument(name = "businessId") UUID businessId) {

        if (businessId != null) {
            // Update scenario: check duplicates but exclude this business
            return slugValidationService.validateSlug(slug, businessId);
        } else {
            // Creation scenario: check for any duplicates
            return slugValidationService.validateSlug(slug, null);
        }
    }
}

