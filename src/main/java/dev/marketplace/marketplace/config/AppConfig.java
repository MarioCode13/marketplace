package dev.marketplace.marketplace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private String baseUrl;
    private Dev dev;

    public static class Dev {
        private Email email;

        public Email getEmail() {
            return email;
        }

        public void setEmail(Email email) {
            this.email = email;
        }

        public static class Email {
            private String override;

            public String getOverride() {
                return override;
            }

            public void setOverride(String override) {
                this.override = override;
            }
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Dev getDev() {
        if (dev == null) {
            dev = new Dev();
        }
        return dev;
    }

    public void setDev(Dev dev) {
        this.dev = dev;
    }
}
