package dev.marketplace.marketplace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payfast")
public class PayFastProperties {
    private boolean enabled = false;
    private String merchantId;
    private String merchantKey;
    private String url;
    private String returnUrl;
    private String cancelUrl;
    private String notifyUrl;
    private String passphrase;
    // When true, require the canonical signature (exclude both merchant_id and merchant_key, RFC3986 encoding)
    private boolean requireSignature = false;
}
