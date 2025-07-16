package dev.marketplace.marketplace.dto;

import dev.marketplace.marketplace.model.Category;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.model.City;

import java.time.LocalDateTime;
import java.util.List;

public record ListingDTO(
        Long id,
        String title,
        String description,
        List<String> images,
        Category category,
        double price,
        City city,
        String customCity,
        String condition,
        User user,
        LocalDateTime createdAt,
        boolean sold,
        String expiresAt
) {}
