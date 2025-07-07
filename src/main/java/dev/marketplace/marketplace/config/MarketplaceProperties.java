package dev.marketplace.marketplace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "marketplace")
public class MarketplaceProperties {
    
    private Security security = new Security();
    private Storage storage = new Storage();
    private Listing listing = new Listing();
    
    @Data
    public static class Security {
        private String jwtSecret;
        private long jwtExpirationMs = 86400000; // 24 hours
        private String corsAllowedOrigins = "http://localhost:3000";
    }
    
    @Data
    public static class Storage {
        private String b2ApplicationKeyId;
        private String b2ApplicationKey;
        private String b2BucketId;
        private String b2BucketName;
        private String baseUrl;
    }
    
    @Data
    public static class Listing {
        private int defaultExpirationDays = 30;
        private int maxImagesPerListing = 10;
        private double maxPrice = 1000000.0;
        private int maxTitleLength = 100;
        private int maxDescriptionLength = 1000;
    }
} 