package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.service.B2StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ImageMutationResolver  {
    private final B2StorageService b2StorageService;

    public ImageMutationResolver(B2StorageService b2StorageService) {
        this.b2StorageService = b2StorageService;
    }

    public String uploadListingImage(MultipartFile image) {
        try {
            return b2StorageService.uploadImage(image);
        } catch (Exception e) {
            throw new RuntimeException("Image upload failed: " + e.getMessage());
        }
    }
}
