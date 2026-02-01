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
                           @Value("${b2.application.key.key}") String applicationKey,
                           @Value("${b2.bucket.id}") String bucketId, 
                           @Value("${b2.bucket.name}") String bucketName) {
        this.bucketId = bucketId;
        this.bucketName = bucketName;

        this.client = B2StorageClientFactory.createDefaultFactory()
                .create(applicationKeyId, applicationKey, "marketplace-app");
    }

    /**
     * Sanitizes the filename to ensure it is safe for B2 and URLs.
     */
    private String sanitizeFilename(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "file";
        }
        return originalName
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9._-]", "");
    }

//    To be deprecated shortly
    public String uploadImage(String fileName, byte[] imageData) throws B2Exception {
        String safeFileName = sanitizeFilename(fileName);

        B2ContentSource contentSource = B2ByteArrayContentSource.builder(imageData).build();

        B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId, safeFileName, B2ContentTypes.B2_AUTO, contentSource)
                .build();

        B2FileVersion uploadedFile = client.uploadSmallFile(request);

        return uploadedFile.getFileName();
    }

    public String generatePreSignedUrl(String fileName) throws B2Exception {
        int validDurationSeconds = 86400;

        B2GetDownloadAuthorizationRequest request = B2GetDownloadAuthorizationRequest
                .builder(bucketId, fileName, validDurationSeconds)
                .build();

        B2DownloadAuthorization auth = client.getDownloadAuthorization(request);

        return "https://f003.backblazeb2.com/file/" + bucketName + "/" + fileName + "?Authorization=" + auth.getAuthorizationToken();
    }
    
    public void deleteImage(String fileName) throws B2Exception {
        B2DeleteFileVersionRequest request = B2DeleteFileVersionRequest
                .builder(fileName, null) // We'll need the fileId for deletion, but fileName should work for most cases
                .build();
        
        try {
            client.deleteFileVersion(request);
        } catch (B2Exception e) {
            // Log the error but don't throw it to avoid breaking the application
            // if the file doesn't exist or can't be deleted
            System.err.println("Failed to delete file from B2: " + fileName + ", error: " + e.getMessage());
        }
    }

    public String uploadPublicImage(String folder, MultipartFile file) throws B2Exception, IOException {
        String safeFileName = sanitizeFilename(file.getOriginalFilename());
        String filePath = folder + "/" + UUID.randomUUID() + "_" + safeFileName;
        return uploadImage(filePath, file.getBytes()); // returns permanent path
    }


}
