package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.IntranetDocument;
import com.example.MardiqueWeb.Service.IntranetDocumentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intranet/documents")
public class IntranetDocumentController {

    private final IntranetDocumentService service;

    public IntranetDocumentController(IntranetDocumentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("sector") String sector,
                                    @RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "uploader", required = false) String uploader) {
        try {
            IntranetDocument doc = service.upload(sector, file, uploader);
            return ResponseEntity.ok(doc);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", "false", "message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("ok", "false", "message", "Error al guardar el archivo."));
        }
    }

    @GetMapping
    public List<IntranetDocument> list(@RequestParam(value = "sector", required = false) String sector) {
        return service.listBySector(sector);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> content(@PathVariable Long id) {
        IntranetDocument doc = service.find(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] data = service.readContent(doc);
            String encoded = URLEncoder.encode(doc.getNombre(), StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(service.contentType(doc.getFileType())))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encoded)
                    .header("X-Previewable", String.valueOf(service.isPreviewable(doc)))
                    .body(data);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/sheet")
    public ResponseEntity<Map<String, Object>> sheet(@PathVariable Long id) {
        IntranetDocument doc = service.find(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        List<List<String>> rows = service.sheetData(doc);
        if (rows == null) {
            return ResponseEntity.badRequest().body(Map.of("rows", List.of(), "colCount", 0));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nombre", doc.getNombre());
        body.put("rows", rows);
        body.put("colCount", rows.isEmpty() ? 0 : rows.get(0).size());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<?> preview(@PathVariable Long id) {
        IntranetDocument doc = service.find(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] pdf = service.previewPdf(doc);
            if (pdf == null) {
                if ("xls".equals(doc.getFileType()) || "xlsx".equals(doc.getFileType())) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_HTML)
                            .body(service.toHtml(doc));
                }
                return ResponseEntity.notFound().build();
            }
            String encoded = URLEncoder.encode(doc.getNombre(), StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encoded)
                    .body(pdf);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/html")
    public ResponseEntity<String> html(@PathVariable Long id) {
        IntranetDocument doc = service.find(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        String html = service.toHtml(doc);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(html);
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@PathVariable Long id) {
        IntranetDocument doc = service.find(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] png = service.thumbnail(doc);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(png);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            response.put("ok", service.delete(id));
        } catch (IOException e) {
            response.put("ok", false);
        }
        return response;
    }
}
