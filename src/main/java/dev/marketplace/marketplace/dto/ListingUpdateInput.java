package dev.marketplace.marketplace.dto;

import dev.marketplace.marketplace.enums.Condition;
import java.util.List;

public record ListingUpdateInput(
        Long id,
        String title,
        Double price,
        String description,
        List<String> images,
        Condition condition,
        Long categoryId,
        Long cityId,
        String customCity
) {}