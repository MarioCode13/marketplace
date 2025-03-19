package dev.marketplace.marketplace.dto;

import java.util.List;

public class ListingPageResponse {
    private List<ListingDTO> listings;
    private int totalCount;

    public ListingPageResponse(List<ListingDTO> listings, int totalCount) {
        this.listings = listings;
        this.totalCount = totalCount;
    }

    public List<ListingDTO> getListings() { // ✅ Fix return type
        return listings;
    }

    public void setListings(List<ListingDTO> listings) {
        this.listings = listings;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}

