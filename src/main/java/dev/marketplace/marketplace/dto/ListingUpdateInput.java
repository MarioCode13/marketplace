package dev.marketplace.marketplace.dto;

import dev.marketplace.marketplace.enums.Condition;
import java.util.List;
import java.util.UUID;

public record ListingUpdateInput(
        UUID id,
        String title,
        Double price,
        String description,
        List<String> images,
        Condition condition,
        UUID categoryId,
        UUID cityId,
        String customCity
) {}