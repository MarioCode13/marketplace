package dev.marketplace.marketplace;

import dev.marketplace.marketplace.service.B2StorageService;
import dev.marketplace.marketplace.config.B2Properties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public B2StorageService mockB2StorageService() {
        // Build B2Properties to match the service constructor
        B2Properties props = new B2Properties();
        B2Properties.Bucket bucket = new B2Properties.Bucket();
        bucket.setId("dummy");
        bucket.setName("dummy");
        props.setBucket(bucket);

        B2Properties.Application app = new B2Properties.Application();
        B2Properties.Application.Key key = new B2Properties.Application.Key();
        key.setId("dummy");
        key.setKey("dummy");
        key.setName("dummy");
        app.setKey(key);
        props.setApplication(app);

        return new B2StorageService(props) {
            @Override
            public String uploadImage(String fileName, byte[] imageData) {
                return "test-uploaded-file-" + fileName;
            }


            @Override
            public String generatePreSignedUrl(String fileName) {
                return "https://test.example.com/file/" + fileName + "?test-auth-token";
            }
        };
    }
}
