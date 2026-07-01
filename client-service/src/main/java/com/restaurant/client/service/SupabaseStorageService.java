package com.restaurant.client.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${supabase.bucket:profile-photos}")
    private String bucket;

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
            log.info("Uploading file {} to Supabase bucket {}", uniqueFilename, bucket);
            
            supabaseRestClient.post()
                    .uri("/object/{bucketName}/{path}", bucket, path)
                    .contentType(MediaType.parseMediaType(file.getContentType() != null ? file.getContentType() : "application/octet-stream"))
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();
            
            log.info("Successfully uploaded file path: {}", path);
            return path;
            
        } catch (IOException e) {
            log.error("Failed to read file for upload", e);
            throw new RuntimeException("Failed to upload image", e);
        } catch (Exception e) {
            log.error("Failed to upload file to Supabase", e);
            throw new RuntimeException("Failed to upload image to Supabase", e);
        }
    }

    public void deleteImage(String pathOrUrl) {
        if (pathOrUrl == null) {
            return;
        }
        
        try {
            String path = pathOrUrl;
            if (pathOrUrl.contains(bucket + "/")) {
                path = pathOrUrl.substring(pathOrUrl.indexOf(bucket + "/") + bucket.length() + 1);
            }
            log.info("Deleting file {} from Supabase bucket {}", path, bucket);
            
            supabaseRestClient.delete()
                    .uri("/object/{bucketName}/{path}", bucket, path)
                    .retrieve()
                    .toBodilessEntity();
                    
            log.info("Successfully deleted file {}", path);
        } catch (Exception e) {
            log.error("Failed to delete file from Supabase: {}", pathOrUrl, e);
        }
    }

    public String getSignedUrl(String path) {
        if (path == null) return null;
        try {
            java.util.Map response = supabaseRestClient.post()
                    .uri("/object/sign/{bucketName}/{path}", bucket, path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(java.util.Map.of("expiresIn", 3600))
                    .retrieve()
                    .body(java.util.Map.class);
            
            if (response != null && response.containsKey("signedURL")) {
                return supabaseUrl + "/storage/v1" + response.get("signedURL");
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to generate signed URL for path: {}", path, e);
            return null;
        }
    }
}
