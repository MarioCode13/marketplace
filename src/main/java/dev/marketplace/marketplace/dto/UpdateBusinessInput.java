package dev.marketplace.marketplace.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateBusinessInput {
    private UUID businessId;
    private String email;
    private String contactNumber;
    private String addressLine1;
    private String addressLine2;
    private UUID cityId;
    private String postalCode;
    private String slug;
}
