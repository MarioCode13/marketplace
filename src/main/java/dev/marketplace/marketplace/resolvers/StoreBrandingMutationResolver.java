package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.StoreBranding;
import dev.marketplace.marketplace.service.StoreBrandingService;
import dev.marketplace.marketplace.service.UserService;
import dev.marketplace.marketplace.dto.UpdateStoreBrandingInput;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

@CrossOrigin(origins = "http://localhost:3000")
@Controller
public class StoreBrandingMutationResolver {
    private final StoreBrandingService storeBrandingService;
    private final UserService userService;

    public StoreBrandingMutationResolver(StoreBrandingService storeBrandingService, UserService userService) {
        this.storeBrandingService = storeBrandingService;
        this.userService = userService;
    }

    @MutationMapping
    public StoreBranding updateStoreBranding(
            @Argument UUID businessId,
            @Argument UpdateStoreBrandingInput input,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = userService.getUserIdByUsername(userDetails.getUsername());
        return storeBrandingService.updateStoreBranding(
            userId,
            businessId,
            input.getSlug(),
            input.getLogoUrl(),
            input.getBannerUrl(),
            input.getThemeColor(),
            input.getPrimaryColor(),
            input.getSecondaryColor(),
            input.getLightOrDark(),
            input.getAbout(),
            input.getStoreName(),
            input.getBackgroundColor(),
            input.getTextColor(),
            input.getCardTextColor()
        );
    }
}
