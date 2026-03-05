package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.ReservedSlug;
import dev.marketplace.marketplace.repository.ReservedSlugRepository;
import dev.marketplace.marketplace.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlugValidationService {

    private final ReservedSlugRepository reservedSlugRepository;
    private final BusinessRepository businessRepository;

    /**
     * Normalize a slug: lowercase, remove diacritics, keep only alphanumeric + hyphens.
     */
    private String normalizeSlug(String input) {
        if (input == null) return null;
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        // remove diacritics
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // replace non-alphanumeric with hyphens
        normalized = normalized.replaceAll("[^\\p{Alnum}]+", "-");
        // collapse hyphens
        normalized = normalized.replaceAll("-+", "-");
        // trim hyphens
        normalized = normalized.replaceAll("^-|-$", "");
        return normalized.toLowerCase();
    }

    /**
     * Levenshtein distance algorithm to calculate string similarity.
     * Returns the minimum number of single-character edits required to change one string to another.
     */
    private int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1),     // insertion
                    dp[i - 1][j - 1] + cost // substitution
                );
            }
        }
        return dp[len1][len2];
    }

    /**
     * Calculate similarity score based on Levenshtein distance.
     * Returns a score from 0 to 1, where 1 is identical.
     */
    private double calculateSimilarity(String s1, String s2) {
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        return 1.0 - (double) distance / maxLength;
    }

    /**
     * Validate a slug against reserved slugs and duplicates.
     *
     * @param slug The slug to validate
     * @param excludeBusinessId Optional business ID to exclude from duplicate check (for updates)
     * @return SlugValidationResult with status and message
     */
    public SlugValidationResult validateSlug(String slug, UUID excludeBusinessId) {
        // First do standard validation
        SlugValidationResult result = validateSlug(slug);

        // If already rejected/invalid, return early
        if (result.getStatus() == SlugValidationResult.SlugStatus.REJECTED ||
            result.getStatus() == SlugValidationResult.SlugStatus.INVALID) {
            return result;
        }

        // Check for duplicate slug in business table
        boolean isDuplicate = excludeBusinessId == null ?
            businessRepository.findBySlug(slug).isPresent() :
            businessRepository.existsBySlugAndIdNot(slug, excludeBusinessId);

        if (isDuplicate) {
            return SlugValidationResult.REJECTED("This slug is already taken. Please choose a different one.");
        }

        return result;
    }

    /**
     * Validate a slug against reserved slugs.
     *
     * @param slug The slug to validate
     * @return SlugValidationResult with status and message
     */
    public SlugValidationResult validateSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return SlugValidationResult.INVALID("Slug cannot be empty");
        }

        // Normalize the slug
        slug = normalizeSlug(slug);

        if (slug == null || slug.isBlank()) {
            return SlugValidationResult.INVALID("Slug must contain at least one valid character");
        }

        // Exact match check
        if (reservedSlugRepository.existsBySlug(slug)) {
            return SlugValidationResult.REJECTED(
                "This slug is reserved and cannot be used. Please choose a different one."
            );
        }

        // Similarity check with configurable threshold
        double similarityThreshold = 0.85;
        List<ReservedSlug> reservedSlugs = reservedSlugRepository.findAll();

        for (ReservedSlug reserved : reservedSlugs) {
            double similarity = calculateSimilarity(slug, reserved.getSlug());

            if (similarity >= similarityThreshold) {
                log.warn(
                    "Slug '{}' is too similar to reserved slug '{}' (similarity: {}). Marking for review.",
                    slug, reserved.getSlug(), String.format("%.2f", similarity)
                );
                return SlugValidationResult.PENDING_REVIEW(
                    "This slug is similar to a reserved brand name. Your request will be reviewed by our team before approval.",
                    reserved.getSlug(),
                    similarity
                );
            }
        }

        // All checks passed
        return SlugValidationResult.APPROVED("Slug is available and approved");
    }

    /**
     * DTO for slug validation results
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class SlugValidationResult {
        private SlugStatus status;
        private String message;
        private String similarTo;  // Which reserved slug it's similar to (null if not applicable)
        private Double similarity;  // Similarity score (null if not applicable)

        public enum SlugStatus {
            APPROVED,        // Slug is available and good to go
            PENDING_REVIEW,  // Slug is too similar to reserved slug, needs manual review
            REJECTED,        // Slug is exactly reserved or otherwise invalid
            INVALID          // Invalid input
        }

        public static SlugValidationResult APPROVED(String message) {
            return SlugValidationResult.builder()
                .status(SlugStatus.APPROVED)
                .message(message)
                .build();
        }

        public static SlugValidationResult PENDING_REVIEW(String message, String similarTo, Double similarity) {
            return SlugValidationResult.builder()
                .status(SlugStatus.PENDING_REVIEW)
                .message(message)
                .similarTo(similarTo)
                .similarity(similarity)
                .build();
        }

        public static SlugValidationResult REJECTED(String message) {
            return SlugValidationResult.builder()
                .status(SlugStatus.REJECTED)
                .message(message)
                .build();
        }

        public static SlugValidationResult INVALID(String message) {
            return SlugValidationResult.builder()
                .status(SlugStatus.INVALID)
                .message(message)
                .build();
        }
    }
}



