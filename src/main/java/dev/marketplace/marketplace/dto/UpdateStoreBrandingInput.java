package dev.marketplace.marketplace.dto;

import lombok.Data;

@Data
public class UpdateStoreBrandingInput {
    private String slug;
    private String logoUrl;
    private String bannerUrl;
    private String themeColor;
    private String primaryColor;
    private String secondaryColor;
    private String lightOrDark;
    private String about;
    private String storeName;
}
