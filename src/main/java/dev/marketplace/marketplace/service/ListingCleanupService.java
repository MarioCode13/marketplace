package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.repository.ListingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ListingCleanupService {
    private final ListingRepository listingRepository;

    public ListingCleanupService(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    // Runs every day at 2am
    @Scheduled(cron = "0 0 2 * * *")
    public void deleteOldArchivedListings() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(14);
        List<Listing> oldArchivedListings = listingRepository.findByArchivedTrueAndCreatedAtBefore(cutoff);
        for (Listing listing : oldArchivedListings) {
            listingRepository.delete(listing);
        }
    }
}

