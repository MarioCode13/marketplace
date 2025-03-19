package dev.marketplace.marketplace.dto;

import dev.marketplace.marketplace.model.Category;
import dev.marketplace.marketplace.model.User;

import java.time.LocalDateTime;
import java.util.List;

public record ListingDTO(
        Long id,
        String title,
        String description,
        List<String> images,  // ✅ Pre-signed image URLs
        Category category,
        double price,
        String location,
        String condition,
        User user,
        LocalDateTime createdAt,
        boolean sold, // ✅ Now included
        String expiresAt // ✅ Now included
) {}
