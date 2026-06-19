package com.example.MardiqueWeb.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String cleanName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "file";
        Map<String, Object> params = ObjectUtils.asMap(
                "resource_type", "raw",
                "public_id", "mardique_" + System.currentTimeMillis() + "_" + cleanName
        );
        Map result = cloudinary.uploader().upload(file.getBytes(), params);
        return (String) result.get("secure_url");
    }

    public String uploadBytes(byte[] data, String originalFileName) throws IOException {
        String cleanName = originalFileName != null
                ? originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_").replaceAll("\\.pdf\\.pdf$", ".pdf")
                : "file";
        Map<String, Object> params = ObjectUtils.asMap(
                "resource_type", "raw",
                "public_id", "mardique_" + System.currentTimeMillis() + "_" + cleanName
        );
        Map result = cloudinary.uploader().upload(data, params);
        return (String) result.get("secure_url");
    }

    public void deleteFile(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
