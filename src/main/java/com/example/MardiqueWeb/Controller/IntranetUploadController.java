package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Service.CloudinaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/intranet/upload")
public class IntranetUploadController {

    private static final Logger log = LoggerFactory.getLogger(IntranetUploadController.class);
    private final CloudinaryService cloudinaryService;

    public IntranetUploadController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El archivo está vacío"));
            }
            String url = cloudinaryService.uploadFile(file);
            String originalName = file.getOriginalFilename();
            log.info("Upload OK (Cloudinary): original={}, url={}, size={}", originalName, url, file.getSize());
            return ResponseEntity.ok(Map.of("url", url, "fileName", originalName));
        } catch (Exception e) {
            log.error("Upload error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error al subir archivo"));
        }
    }
}
