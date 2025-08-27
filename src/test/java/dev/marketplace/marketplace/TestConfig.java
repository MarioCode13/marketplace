package dev.marketplace.marketplace;

import dev.marketplace.marketplace.service.B2StorageService;
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
        return new B2StorageService("dummy", "dummy", "dummy", "dummy") {
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
