package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.repository.ReservedSlugRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SlugValidationService.
 * Tests reserved slug protection, similarity detection, and normalization.
 */
@SpringBootTest
public class SlugValidationServiceIntegrationTest {

    @Autowired
    private SlugValidationService slugValidationService;

    @Autowired
    private ReservedSlugRepository reservedSlugRepository;

    @BeforeEach
    public void setUp() {
        // Reserved slugs are seeded by migration script
        // Verify that at least some reserved slugs are present
        assertTrue(reservedSlugRepository.existsBySlug("google"), "Reserved slugs should be seeded");
    }

    // === EXACT MATCH TESTS ===

    @Test
    public void testReservedSlugExactMatchIsRejected() {
        // Test a well-known brand from our reserved list
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("google");

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.REJECTED, result.getStatus());
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().toLowerCase().contains("reserved"));
    }

    @Test
    public void testSouthAfricanBrandRejected() {
        // Test a South African brand from our reserved list
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("takealot");

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.REJECTED, result.getStatus());
    }

    @Test
    public void testPlatformReservedSlugRejected() {
        // Test platform-reserved word
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("admin");

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.REJECTED, result.getStatus());
    }

    // === SIMILARITY DETECTION TESTS ===

    @Test
    public void testSimilarSlugPendingReview() {
        // "goggle" is very similar to "google"
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("goggle");

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.PENDING_REVIEW, result.getStatus());
        assertEquals("google", result.getSimilarTo());
        assertNotNull(result.getSimilarity());
        assertTrue(result.getSimilarity() >= 0.85, "Similarity should meet threshold");
    }

    @Test
    public void testSimilarToTakealotPendingReview() {
        // "takealott" is similar to "takealot" (SA brand)
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("takealott");

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.PENDING_REVIEW, result.getStatus());
        assertEquals("takealot", result.getSimilarTo());
    }

    // === NORMALIZATION TESTS ===

    @Test
    public void testSlugNormalizationWithSpaces() {
        // "my brand" normalizes to "my-brand"
        SlugValidationService.SlugValidationResult result1 = slugValidationService.validateSlug("my brand");
        SlugValidationService.SlugValidationResult result2 = slugValidationService.validateSlug("my-brand");

        // Both should have same result (approved for this slug)
        assertEquals(result1.getStatus(), result2.getStatus());
    }

    @Test
    public void testSlugNormalizationWithDiacritics() {
        // "café" normalizes to "cafe"
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("café-store");

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.APPROVED, result.getStatus());
    }

    @Test
    public void testSlugNormalizationCaseInsensitive() {
        // "GOOGLE" normalizes to "google" (reserved)
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("GOOGLE");

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.REJECTED, result.getStatus());
    }

    @Test
    public void testSlugNormalizationSpecialCharacters() {
        // "my-brand@123" normalizes to "my-brand-123"
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("my-brand@123");

        // Should be approved as it's unique
        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.APPROVED, result.getStatus());
    }

    // === VALID SLUG TESTS ===

    @Test
    public void testUniqueSlugApproved() {
        // A completely unique slug should be approved
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("my-unique-store-xyz");

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.APPROVED, result.getStatus());
        assertNull(result.getSimilarTo());
        assertNull(result.getSimilarity());
    }

    @Test
    public void testBusinessNameStyleSlugApproved() {
        // A typical business name style slug
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("johans-handmade-crafts");

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.APPROVED, result.getStatus());
    }

    @Test
    public void testNumbersInSlugApproved() {
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("store123-abc");

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.APPROVED, result.getStatus());
    }

    // === EDGE CASE TESTS ===

    @Test
    public void testEmptySlugIsInvalid() {
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("");

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.INVALID, result.getStatus());
    }

    @Test
    public void testNullSlugIsInvalid() {
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug(null);

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.INVALID, result.getStatus());
    }

    @Test
    public void testWhitespaceOnlySlugIsInvalid() {
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("   ");

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.INVALID, result.getStatus());
    }

    @Test
    public void testSlugWithOnlySpecialCharactersIsInvalid() {
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("@#$%");

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.INVALID, result.getStatus());
    }

    // === MESSAGE CONTENT TESTS ===

    @Test
    public void testApprovedMessageContent() {
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("my-store");

        assertTrue(result.getMessage().toLowerCase().contains("available"));
    }

    @Test
    public void testRejectedMessageContent() {
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("google");

        assertTrue(result.getMessage().toLowerCase().contains("reserved"));
    }

    @Test
    public void testPendingReviewMessageContent() {
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("goggle");

        assertTrue(result.getMessage().toLowerCase().contains("review"));
    }

    @Test
    public void testPendingReviewIncludesSimilarSlug() {
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("goggle");

        assertEquals("google", result.getSimilarTo());
        assertTrue(result.getSimilarity() > 0);
    }

    // === UPDATE SCENARIO TESTS (with excludeBusinessId) ===

    @Test
    public void testUpdateWithSameSlugAllowed() {
        // When updating a business with its own slug, it should be allowed
        UUID businessId = UUID.randomUUID();
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("my-store", businessId);

        // Should not be rejected as duplicate since we're excluding this business
        assertNotEquals(SlugValidationService.SlugValidationResult.SlugStatus.REJECTED, result.getStatus());
    }

    @Test
    public void testUpdateReservedSlugStillRejected() {
        // Reserved slugs should still be rejected even during update
        UUID businessId = UUID.randomUUID();
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("google", businessId);

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.REJECTED, result.getStatus());
        assertTrue(result.getMessage().toLowerCase().contains("reserved"));
    }

    @Test
    public void testUpdateSimilarSlugStillPendingReview() {
        // Similar slugs should still be flagged during update
        UUID businessId = UUID.randomUUID();
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("goggle", businessId);

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.PENDING_REVIEW, result.getStatus());
        assertEquals("google", result.getSimilarTo());
    }

    @Test
    public void testUpdateWithUniqueSlugApproved() {
        // Unique slug should be approved for update
        UUID businessId = UUID.randomUUID();
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("updated-unique-slug", businessId);

        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.APPROVED, result.getStatus());
    }

    @Test
    public void testUpdateWithNullBusinessIdChecksDuplicates() {
        // When businessId is null, it should check for duplicates
        SlugValidationService.SlugValidationResult result = slugValidationService.validateSlug("my-unique-slug", null);

        // Should be approved since slug doesn't exist yet
        assertEquals(SlugValidationService.SlugValidationResult.SlugStatus.APPROVED, result.getStatus());
    }
}
