package dev.marketplace.marketplace.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ListingPageResponse {
    private List<ListingDTO> listings;
    private int totalCount;

    public ListingPageResponse(List<ListingDTO> listings, int totalCount) {
        this.listings = listings;
        this.totalCount = totalCount;
    }

}

