package dev.marketplace.marketplace.dto;

import java.util.UUID;

public class EmailRequest {
    private String to; // Deprecated: use sellerId instead
    private UUID sellerId; // New: seller's user ID (email looked up server-side)
    private String subject;
    private String body;

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public UUID getSellerId() { return sellerId; }
    public void setSellerId(UUID sellerId) { this.sellerId = sellerId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return body; }
    public void setMessage(String message) { this.body = message; }
}