package dev.marketplace.marketplace.dto;

import lombok.Data;

@Data
public class UpdateBusinessInput {
    private Long businessId;
    private String email;
    private String contactNumber;
    private String addressLine1;
    private String addressLine2;
    private Long cityId;
    private String postalCode;
}
