package dev.marketplace.marketplace.dto;

import dev.marketplace.marketplace.model.Category;
import dev.marketplace.marketplace.dto.UserDTO;
import dev.marketplace.marketplace.model.City;
import dev.marketplace.marketplace.model.Business;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ListingDTO(
        UUID id,
        String title,
        String description,
        List<String> images,
        Category category,
        double price,
        int quantity,
        City city,
        String customCity,
        String condition,
        UserDTO user,
        Business business,
        LocalDateTime createdAt,
        boolean sold,
        String expiresAt,
        boolean archived // Added
) {}
