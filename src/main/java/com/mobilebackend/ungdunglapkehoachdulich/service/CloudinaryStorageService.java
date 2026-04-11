package com.mobilebackend.ungdunglapkehoachdulich.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
}
