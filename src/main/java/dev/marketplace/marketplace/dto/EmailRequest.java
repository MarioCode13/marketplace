package dev.marketplace.marketplace.dto;

import java.util.UUID;

public class EmailRequest {
    private String to; // Deprecated: use sellerId instead
    private UUID sellerId; // New: seller's user ID (email looked up server-side)
    private String subject;
    private String body;
    // Optional sender info to support nicer templated emails
    private String fromEmail;
    private String fromName;
    // Optional listing info for templated contact emails
    private String listingTitle;

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public UUID getSellerId() { return sellerId; }
    public void setSellerId(UUID sellerId) { this.sellerId = sellerId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return body; }
    public void setMessage(String message) { this.body = message; }

    public String getFromEmail() { return fromEmail; }
    public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public String getListingTitle() { return listingTitle; }
    public void setListingTitle(String listingTitle) { this.listingTitle = listingTitle; }
}