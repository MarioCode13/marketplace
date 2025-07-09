package dev.marketplace.marketplace.dto;

public class EmailRequest {
    private String to;
    private String subject;
    private String body;

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return body; }
    public void setMessage(String message) { this.body = message; }
}