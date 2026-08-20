package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.IntranetDocument;
import com.example.MardiqueWeb.Service.IntranetAccessService;
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
    private final IntranetAccessService accessService;

    public IntranetDocumentController(IntranetDocumentService service,
                                      IntranetAccessService accessService) {
        this.service = service;
        this.accessService = accessService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("sector") String sector,
                                    @RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "uploader", required = false) String uploader,
                                    @RequestParam(value = "parentId", required = false) Long parentId) {
        try {
            IntranetDocument doc = service.upload(sector, file, uploader, parentId);
            return ResponseEntity.ok(doc);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", "false", "message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("ok", "false", "message", "Error al guardar el archivo."));
        }
    }

    @PostMapping("/folder")
    public ResponseEntity<?> createFolder(@RequestParam("sector") String sector,
                                          @RequestParam("nombre") String nombre,
                                          @RequestParam(value = "uploader", required = false) String uploader,
                                          @RequestParam(value = "parentId", required = false) Long parentId) {
        try {
            IntranetDocument folder = service.createFolder(sector, nombre, uploader, parentId);
            return ResponseEntity.ok(folder);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", "false", "message", e.getMessage()));
        }
    }

    @GetMapping
    public List<IntranetDocument> list(@RequestParam(value = "sector", required = false) String sector,
                                       @RequestParam(value = "parentId", required = false) Long parentId,
                                       @RequestParam(value = "all", required = false, defaultValue = "false") boolean all) {
        if (parentId != null) {
            return service.listByParent(parentId);
        }
        if (all) {
            return service.listAllBySector(sector);
        }
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

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> qr(@PathVariable Long id,
                                     jakarta.servlet.http.HttpServletRequest request) {
        IntranetDocument doc = service.find(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        // El QR abre la página de documentos y previsualiza el documento
        // (la persona debe dar clic en "Abrir"/"Descargar" para que se registre la vista).
        String url = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() != 80 && request.getServerPort() != 443
                ? ":" + request.getServerPort() : "")
                + request.getContextPath()
                + "/intranet/documentos?documento=" + id + "&modo_prueba=true";
        byte[] png = service.qrPng(doc, url);
        if (png == null) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(png);
    }

    /* ---------- Registro de vistas ---------- */

    @PostMapping("/{id}/view")
    public ResponseEntity<?> recordView(@PathVariable Long id,
                                        @RequestBody(required = false) Map<String, String> body) {
        IntranetDocument doc = service.find(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        String email = body == null ? null : body.get("email");
        String name = body == null ? null : body.get("name");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", "false", "message", "Se requiere el correo del usuario."));
        }
        accessService.recordView(id, email, name);
        return ResponseEntity.ok(Map.of("ok", "true"));
    }

    @GetMapping("/{id}/views")
    public ResponseEntity<?> documentViews(@PathVariable Long id,
                                           @RequestParam(value = "email", required = false) String email) {
        IntranetDocument doc = service.find(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        if (!accessService.canViewViewers(email)) {
            return ResponseEntity.status(403).body(Map.of("ok", "false", "message", "No tienes permiso para ver esta información."));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("documentId", doc.getId());
        body.put("documentName", doc.getNombre());
        body.put("sector", doc.getSector());
        body.put("views", accessService.viewsByDocument(id));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/views/events")
    public ResponseEntity<?> viewEvents(@RequestParam(value = "email", required = false) String email) {
        if (!accessService.isAdmin(email)) {
            return ResponseEntity.status(403).body(Map.of("ok", "false", "message", "Solo el administrador de la intranet puede ver este registro."));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("events", accessService.allViewEvents());
        return ResponseEntity.ok(body);
    }

    /* ---------- Panel de administración de la intranet ---------- */

    @GetMapping("/views/access")
    public ResponseEntity<?> accessStatus(@RequestParam(value = "email", required = false) String email) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("isAdmin", accessService.isAdmin(email));
        body.put("canViewViewers", accessService.canViewViewers(email));
        body.put("testMode", accessService.isTestMode());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/views/stats")
    public ResponseEntity<?> viewStats(@RequestParam(value = "email", required = false) String email) {
        if (!accessService.isAdmin(email)) {
            return ResponseEntity.status(403).body(Map.of("ok", "false", "message", "Solo el administrador de la intranet puede ver esta información."));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("totalDocuments", accessService.totalDocuments());
        body.put("totalViews", accessService.totalViews());
        body.put("adminEmails", accessService.listByRole("ADMIN"));
        body.put("viewerEmails", accessService.listByRole("VIEWER"));
        body.put("testMode", accessService.isTestMode());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/views/grant")
    public ResponseEntity<?> grant(@RequestParam("email") String email,
                                   @RequestParam("role") String role,
                                   @RequestParam(value = "adminEmail", required = false) String adminEmail) {
        if (!accessService.isAdmin(adminEmail)) {
            return ResponseEntity.status(403).body(Map.of("ok", "false", "message", "Solo el administrador puede otorgar permisos."));
        }
        if (!"ADMIN".equals(role) && !"VIEWER".equals(role)) {
            return ResponseEntity.badRequest().body(Map.of("ok", "false", "message", "Rol no válido."));
        }
        accessService.grantRole(email, role);
        return ResponseEntity.ok(Map.of("ok", "true"));
    }

    @DeleteMapping("/views/revoke")
    public ResponseEntity<?> revoke(@RequestParam("email") String email,
                                    @RequestParam(value = "adminEmail", required = false) String adminEmail) {
        if (!accessService.isAdmin(adminEmail)) {
            return ResponseEntity.status(403).body(Map.of("ok", "false", "message", "Solo el administrador puede quitar permisos."));
        }
        accessService.revokeRole(email);
        return ResponseEntity.ok(Map.of("ok", "true"));
    }

    @GetMapping("/qr/sector/{sector}")
    public ResponseEntity<byte[]> sectorQr(@PathVariable String sector,
                                           jakarta.servlet.http.HttpServletRequest request) {
        String url = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() != 80 && request.getServerPort() != 443
                ? ":" + request.getServerPort() : "")
                + request.getContextPath()
                + "/intranet/documentos?sector=" + service.sanitizeSector(sector) + "&modo_prueba=true";
        byte[] png = service.qrPng(null, url);
        if (png == null) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(png);
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
