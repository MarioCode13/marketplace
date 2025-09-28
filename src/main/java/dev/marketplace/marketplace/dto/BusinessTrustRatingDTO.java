package dev.marketplace.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessTrustRatingDTO {
    private double averageRating;
    private int reviewCount;
}

