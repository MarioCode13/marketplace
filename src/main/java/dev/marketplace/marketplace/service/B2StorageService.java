package dev.marketplace.marketplace.service;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2GetUploadUrlRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadUrlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class B2StorageService {

    private final B2StorageClient client;

    @Value("${b2.bucket.name}")
    private String bucketName;

    public B2StorageService(@Value("${b2.application.key.id}") String keyId,
                            @Value("${b2.application.key}") String key) {
        if (keyId == null || key.isEmpty() || key == null  ) {
            throw new RuntimeException("B2 credentials are missing!");
        }

        System.out.println("Initializing B2 Client with keyId: ");

        // Initialize the B2StorageClient
        this.client = B2StorageClientFactory.createDefaultFactory()
                .create(keyId, key, "marketplace-app");
    }

    public String uploadImage(MultipartFile file) throws IOException, B2Exception {
        String fileName = "listings/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        // Convert MultipartFile to B2ContentSource
        B2ContentSource contentSource = B2ByteArrayContentSource.builder(file.getBytes()).build();

        // Create upload request
        B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketName, fileName, B2ContentTypes.B2_AUTO, contentSource)
                .build();

        // Upload file
        B2FileVersion uploadedFile = client.uploadSmallFile(request);

        // Return public file URL
        return "https://f000.backblazeb2.com/file/" + bucketName + "/" + uploadedFile.getFileName();
    }
}
