package dev.marketplace.marketplace.service;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Service
public class B2StorageService {

    private final B2StorageClient client;

    private final String bucketId;
    private final String bucketName;

    public B2StorageService(@Value("${b2.application.key.id}") String keyId,
                            @Value("${b2.application.key}") String key,
                            @Value("${b2.bucket.id}") String bucketId,
                            @Value("${b2.bucket.name}") String bucketName){
        if (keyId == null || key.isEmpty() || key == null  ) {
            throw new RuntimeException("B2 credentials are missing!");
        }

        System.out.println("Initializing B2 Client with keyId: ");

        // Initialize the B2StorageClient
        this.client = B2StorageClientFactory.createDefaultFactory()
                .create(keyId, key, "marketplace-app");

        this.bucketId = bucketId;  // Use the bucket ID from properties
        this.bucketName = bucketName;
    }
    public String uploadImage(MultipartFile file) throws IOException, B2Exception {
        String fileName = "listings/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        B2ContentSource contentSource = B2ByteArrayContentSource.builder(file.getBytes()).build();
        B2UploadFileRequest request = B2UploadFileRequest.builder(bucketId, fileName, B2ContentTypes.B2_AUTO, contentSource).build();
        B2FileVersion uploadedFile = client.uploadSmallFile(request);

        // ✅ Store only the file name (not full URL)
        return fileName;
    }

    public String generatePreSignedUrl(String fileName) throws B2Exception {
        int validDurationSeconds = 86400; // 24 hours validity

        B2DownloadAuthorization auth = client.getDownloadAuthorization(
                B2GetDownloadAuthorizationRequest.builder(bucketId, fileName, validDurationSeconds).build()
        );

        // ✅ Get download URL
        return "https://f003.backblazeb2.com/file/" + bucketName + "/" + fileName + "?Authorization=" + auth.getAuthorizationToken();
    }


//    public String uploadImage(MultipartFile file) throws IOException, B2Exception {
//        String fileName = "listings/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
//
//        // Convert MultipartFile to B2ContentSource
//        B2ContentSource contentSource = B2ByteArrayContentSource.builder(file.getBytes()).build();
//
//        // Create upload request
//        // ✅ Use bucket ID instead of bucket name
//        B2UploadFileRequest request = B2UploadFileRequest
//                .builder(bucketId, fileName, B2ContentTypes.B2_AUTO, contentSource)
//                .build();
//
//        // Upload file
//        B2FileVersion uploadedFile = client.uploadSmallFile(request);
//
//        // Return public file URL
//        return "https://f003.backblazeb2.com/file/" + bucketName + "/" + uploadedFile.getFileName();
//    }

}
