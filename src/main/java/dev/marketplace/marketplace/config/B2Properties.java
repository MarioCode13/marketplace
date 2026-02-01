package dev.marketplace.marketplace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "b2")
public class B2Properties {
    private Application application;
    private Bucket bucket;

    @Setter
    @Getter
    public static class Application {
        private Key key;

        @Setter
        @Getter
        public static class Key {
            private String id;
            private String name;
            private String key;

        }

    }

    @Setter
    @Getter
    public static class Bucket {
        private String id;
        private String name;

    }

}
