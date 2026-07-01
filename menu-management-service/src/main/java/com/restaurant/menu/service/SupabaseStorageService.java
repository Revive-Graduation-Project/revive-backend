package com.restaurant.menu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseStorageService {

    private final RestClient supabaseRestClient;

    @Value("${supabase.url}")
    private String supabaseUrl;

    private static final String BUCKET_NAME = "meal-photos";

    /**
     * Uploads an image to Supabase Storage.
     * @param file The file to upload
     * @return The public URL of the uploaded image
     */
    private static final java.util.Set<String> ALLOWED_MIME_TYPES = java.util.Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty or null");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file format. Allowed formats are: JPG, JPEG, PNG, WEBP");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        String path = uniqueFilename;

        try {
            log.info("Uploading file {} to Supabase bucket {}", uniqueFilename, BUCKET_NAME);
            
            // For simple REST Client upload, we send the raw bytes with the correct content type
            supabaseRestClient.post()
                    .uri("/object/{bucketName}/{path}", BUCKET_NAME, path)
                    .contentType(MediaType.parseMediaType(file.getContentType() != null ? file.getContentType() : "application/octet-stream"))
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();
            
            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + BUCKET_NAME + "/" + path;
            log.info("Successfully uploaded file. Public URL: {}", publicUrl);
            return publicUrl;
            
        } catch (IOException e) {
            log.error("Failed to read file for upload", e);
            throw new RuntimeException("Failed to upload image", e);
        } catch (Exception e) {
            log.error("Failed to upload file to Supabase", e);
            throw new RuntimeException("Failed to upload image to Supabase", e);
        }
    }

    /**
     * Deletes an image from Supabase Storage using its public URL.
     * @param publicUrl The public URL of the image to delete
     */
    public void deleteImage(String publicUrl) {
        if (publicUrl == null || !publicUrl.contains(BUCKET_NAME + "/")) {
            return;
        }
        
        try {
            String path = publicUrl.substring(publicUrl.indexOf(BUCKET_NAME + "/") + BUCKET_NAME.length() + 1);
            log.info("Deleting file {} from Supabase bucket {}", path, BUCKET_NAME);
            
            supabaseRestClient.delete()
                    .uri("/object/{bucketName}/{path}", BUCKET_NAME, path)
                    .retrieve()
                    .toBodilessEntity();
                    
            log.info("Successfully deleted file {}", path);
        } catch (Exception e) {
            log.error("Failed to delete file from Supabase: {}", publicUrl, e);
            // We don't throw here to ensure we can still delete the meal or update the photo
            // even if the Supabase deletion fails.
        }
    }
}
