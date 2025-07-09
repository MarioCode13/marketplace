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

    public B2StorageService(@Value("${b2.application.key.id}") String applicationKeyId, 
                           @Value("${b2.application.key}") String applicationKey, 
                           @Value("${b2.bucket.id}") String bucketId, 
                           @Value("${b2.bucket.name}") String bucketName) {
        this.bucketId = bucketId;
        this.bucketName = bucketName;

        this.client = B2StorageClientFactory.createDefaultFactory()
                .create(applicationKeyId, applicationKey, "marketplace-app");
    }

    public String uploadImage(String fileName, byte[] imageData) throws B2Exception {
        B2ContentSource contentSource = B2ByteArrayContentSource.builder(imageData).build();

        B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId, fileName, B2ContentTypes.B2_AUTO, contentSource)
                .build();

        B2FileVersion uploadedFile = client.uploadSmallFile(request);

        return uploadedFile.getFileName();
    }

    public String uploadImage(MultipartFile file) throws B2Exception, IOException {
        String fileName = "listings/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        byte[] imageData = file.getBytes();
        return uploadImage(fileName, imageData);
    }

    public String generatePreSignedUrl(String fileName) throws B2Exception {
        int validDurationSeconds = 86400;

        B2GetDownloadAuthorizationRequest request = B2GetDownloadAuthorizationRequest
                .builder(bucketId, fileName, validDurationSeconds)
                .build();

        B2DownloadAuthorization auth = client.getDownloadAuthorization(request);

        return "https://f003.backblazeb2.com/file/" + bucketName + "/" + fileName + "?Authorization=" + auth.getAuthorizationToken();
    }
}
