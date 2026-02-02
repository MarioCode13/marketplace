package dev.marketplace.marketplace.service;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.*;
import dev.marketplace.marketplace.config.B2Properties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class B2StorageService {

    private final B2StorageClient client;
    private final String bucketId;
    private final String bucketName;

    public B2StorageService(B2Properties props) {
        // Prefer values from configuration properties
        String resolvedBucketId = null;
        String resolvedBucketName = null;
        String appKeyId = null;
        String appKeyName = null;
        String appKeySecret = null;

        if (props != null) {
            if (props.getBucket() != null) {
                resolvedBucketId = props.getBucket().getId();
                resolvedBucketName = props.getBucket().getName();
            }
            if (props.getApplication() != null && props.getApplication().getKey() != null) {
                B2Properties.Application.Key k = props.getApplication().getKey();
                appKeyId = k.getId();
                appKeyName = k.getName();
                appKeySecret = k.getKey();
            }
        }

        // Fallback to environment variables if any of the above are missing
        if (appKeyId == null || appKeyId.isBlank()) {
            appKeyId = System.getenv("B2_APPLICATION_KEY_ID");
        }
        if (appKeyName == null || appKeyName.isBlank()) {
            appKeyName = System.getenv("B2_APPLICATION_KEY_NAME");
        }
        if (appKeySecret == null || appKeySecret.isBlank()) {
            // Support two environment naming styles: B2_APPLICATION_KEY_KEY (nested 'key.key')
            // and the historical B2_APPLICATION_KEY which many deployments set.
            appKeySecret = System.getenv("B2_APPLICATION_KEY_KEY");
            if (appKeySecret == null || appKeySecret.isBlank()) {
                appKeySecret = System.getenv("B2_APPLICATION_KEY");
            }
        }

        if (resolvedBucketId == null || resolvedBucketId.isBlank()) {
            resolvedBucketId = System.getenv("B2_BUCKET_ID");
        }
        if (resolvedBucketName == null || resolvedBucketName.isBlank()) {
            resolvedBucketName = System.getenv("B2_BUCKET_NAME");
        }

        this.bucketId = resolvedBucketId;
        this.bucketName = resolvedBucketName;

        this.client = B2StorageClientFactory.createDefaultFactory()
                .create(appKeyId, appKeySecret, "marketplace-app");
    }

    private String sanitizeFilename(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "file";
        }
        return originalName
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9._-]", "");
    }

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
                .builder(fileName, null)
                .build();

        try {
            client.deleteFileVersion(request);
        } catch (B2Exception e) {
            System.err.println("Failed to delete file from B2: " + fileName + ", error: " + e.getMessage());
        }
    }

    public String uploadPublicImage(String folder, MultipartFile file) throws B2Exception, IOException {
        String safeFileName = sanitizeFilename(file.getOriginalFilename());
        String filePath = folder + "/" + UUID.randomUUID() + "_" + safeFileName;
        return uploadImage(filePath, file.getBytes());
    }
}
