package yenha.foodstore.Menu.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    
    private final S3Client s3Client;
    
    @Value("${AWS_S3_BUCKET_NAME}")
    private String bucketName;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(MultipartFile file) {
        logger.info("Starting file upload process for file: {}", file.getOriginalFilename());

        validateFile(file);
        
        String fileName = generateFileName(file.getOriginalFilename());
        
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileName)
                            .contentType(file.getContentType())
                            .build(),
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes())
            );

            String fileUrl = "https://" + bucketName + ".s3.amazonaws.com/" + fileName;
            logger.info("File uploaded successfully to S3: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            logger.error("IO error while uploading file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to read file data: " + e.getMessage());
        } catch (S3Exception e) {
            logger.error("S3 error while uploading file: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to S3: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while uploading file: {}", e.getMessage());
            throw new RuntimeException("Unexpected error during file upload: " + e.getMessage());
        }
    }
    
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        
        try {
            String fileName = extractFileNameFromUrl(fileUrl);
            if (fileName != null) {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .build());
                logger.info("File deleted successfully from S3: {}", fileName);
            }
        } catch (S3Exception e) {
            logger.error("Failed to delete file from S3: {}", e.getMessage());
        }
    }
    
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Only JPEG, PNG, GIF, and WebP images are allowed");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !hasValidImageExtension(originalFilename)) {
            throw new IllegalArgumentException("Invalid file extension. Only .jpg, .jpeg, .png, .gif, .webp are allowed");
        }
        
        logger.debug("File validation passed for: {}", originalFilename);
    }
    
    private boolean hasValidImageExtension(String filename) {
        String lowerCaseFilename = filename.toLowerCase();
        return lowerCaseFilename.endsWith(".jpg") || 
               lowerCaseFilename.endsWith(".jpeg") || 
               lowerCaseFilename.endsWith(".png") || 
               lowerCaseFilename.endsWith(".gif") || 
               lowerCaseFilename.endsWith(".webp");
    }
    
    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }
    
    private String extractFileNameFromUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains(bucketName)) {
            return null;
        }
        
        try {
            return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        } catch (Exception e) {
            logger.warn("Failed to extract filename from URL: {}", fileUrl);
            return null;
        }
    }
}
