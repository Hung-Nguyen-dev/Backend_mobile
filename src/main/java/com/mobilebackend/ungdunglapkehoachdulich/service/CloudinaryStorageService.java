package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Service for handling image uploads to cloud storage.
 * Currently uses local file storage as a fallback.
 * Can be extended to integrate with Cloudinary or other cloud services.
 */
@Service
public class CloudinaryStorageService {

    private static final String UPLOAD_DIR = "uploads";

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    /**
     * Uploads an image file to the cloud storage (or local storage).
     *
     * @param file the multipart file to upload
     * @param folder the folder/category for organizing uploads (e.g., "avatars")
     * @return the URL/path of the uploaded image
     * @throws IOException if an I/O error occurs
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (isCloudinaryConfigured()) {
            try {
                Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                        "cloud_name", cloudName,
                        "api_key", apiKey,
                        "api_secret", apiSecret
                ));

                Map<?, ?> uploadResult = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", folder,
                                "resource_type", "image"
                        )
                );

                Object secureUrl = uploadResult.get("secure_url");
                if (secureUrl != null) {
                    return secureUrl.toString();
                }
            } catch (Exception ignored) {
                // Fallback local storage when Cloudinary upload fails.
            }
        }

        // Generate unique filename to avoid conflicts
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        
        // Create folder path
        Path folderPath = Paths.get(UPLOAD_DIR, folder);
        Files.createDirectories(folderPath);

        // Save file
        Path filePath = folderPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        // Return URL path (can be adjusted based on your web server configuration)
        return "/" + UPLOAD_DIR + "/" + folder + "/" + fileName;
    }

    /**
     * Deletes an image file from cloud storage.
     *
     * @param fileUrl the URL/path of the file to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteImage(String fileUrl) {
        try {
            if (fileUrl != null && fileUrl.contains("res.cloudinary.com") && isCloudinaryConfigured()) {
                Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                        "cloud_name", cloudName,
                        "api_key", apiKey,
                        "api_secret", apiSecret
                ));

                String marker = "/upload/";
                int markerIndex = fileUrl.indexOf(marker);
                if (markerIndex > -1) {
                    String publicIdPart = fileUrl.substring(markerIndex + marker.length());
                    publicIdPart = publicIdPart.replaceAll("^v\\d+/", "");
                    int dotIndex = publicIdPart.lastIndexOf('.');
                    String publicId = dotIndex > -1 ? publicIdPart.substring(0, dotIndex) : publicIdPart;
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                    return true;
                }
            }

            // Convert URL path back to file path
            String filePath = fileUrl.substring(1); // Remove leading '/'
            Path path = Paths.get(filePath);
            
            if (Files.exists(path)) {
                Files.delete(path);
                return true;
            }
            return false;
        } catch (IOException e) {
            System.err.println("Error deleting file: " + e.getMessage());
            return false;
        }
    }

    private boolean isCloudinaryConfigured() {
        return !isBlank(cloudName) && !isBlank(apiKey) && !isBlank(apiSecret);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
