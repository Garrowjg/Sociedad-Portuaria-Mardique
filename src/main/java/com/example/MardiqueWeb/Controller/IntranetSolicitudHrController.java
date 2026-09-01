package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.SolicitudHr;
import com.example.MardiqueWeb.Entity.SolicitudHrRecipient;
import com.example.MardiqueWeb.Entity.UserSignature;
import com.example.MardiqueWeb.Repository.SolicitudHrRepository;
import com.example.MardiqueWeb.Repository.SolicitudHrRecipientRepository;
import com.example.MardiqueWeb.Repository.UserSignatureRepository;
import com.example.MardiqueWeb.Service.CloudinaryService;
import com.example.MardiqueWeb.Service.ExcelToPdfService;
import com.example.MardiqueWeb.Service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/intranet/solicitudes-hr")
public class IntranetSolicitudHrController {

    private static final Logger log = LoggerFactory.getLogger(IntranetSolicitudHrController.class);

    private final SolicitudHrRepository solRepo;
    private final SolicitudHrRecipientRepository recipRepo;
    private final UserSignatureRepository sigRepo;
    private final PdfService pdfService;
    private final CloudinaryService cloudinaryService;
    private final ExcelToPdfService excelToPdfService;

    public IntranetSolicitudHrController(SolicitudHrRepository solRepo,
                                         SolicitudHrRecipientRepository recipRepo,
                                         UserSignatureRepository sigRepo,
                                         PdfService pdfService,
                                         CloudinaryService cloudinaryService,
                                         ExcelToPdfService excelToPdfService) {
        this.solRepo = solRepo;
        this.recipRepo = recipRepo;
        this.sigRepo = sigRepo;
        this.pdfService = pdfService;
        this.cloudinaryService = cloudinaryService;
        this.excelToPdfService = excelToPdfService;
    }

    public static final Map<String, String> STAFF_DIRECTORY = Map.of(
        "AdelinoAragon@spmardique.co", "Adelino Aragon",
        "johnnier.mardique@mardique.com", "Johnnier Admin",
        "gerente-general@mardique.com", "Gerente General",
        "gerente-operaciones@mardique.com", "Gerente de Operaciones",
        "gerente-comercial@mardique.com", "Gerente Comercial",
        "gerente-financiera@mardique.com", "Gerente Financiera",
        "coordinador-sistemas@mardique.com", "Coordinador de Sistemas",
        "coordinador-th@mardique.com", "Coordinador de Talento Humano",
        "talento-humano@mardique.com", "Talento Humano"
    );

    public static final Map<String, String> STAFF_ROLES = Map.of(
        "AdelinoAragon@spmardique.co", "Coordinador de Sistemas",
        "johnnier.mardique@mardique.com", "Administrador",
        "gerente-general@mardique.com", "Gerente General",
        "gerente-operaciones@mardique.com", "Gerente de Operaciones",
        "gerente-comercial@mardique.com", "Gerente Comercial",
        "gerente-financiera@mardique.com", "Gerente Financiera",
        "coordinador-sistemas@mardique.com", "Coordinador de Sistemas",
        "coordinador-th@mardique.com", "Coordinador de Talento Humano",
        "talento-humano@mardique.com", "Talento Humano"
    );

    @GetMapping("/staff")
    public List<Map<String, String>> getStaffDirectory() {
        List<Map<String, String>> list = new ArrayList<>();
        STAFF_DIRECTORY.forEach((email, name) -> {
            Map<String, String> item = new HashMap<>();
            item.put("email", email);
            item.put("name", name);
            item.put("role", STAFF_ROLES.getOrDefault(email, ""));
            list.add(item);
        });
        return list;
    }

    @PostMapping
    public ResponseEntity<?> createSolicitud(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("sectionId") String sectionId,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false, defaultValue = "") String description,
            @RequestParam("senderEmail") String senderEmail,
            @RequestParam("senderName") String senderName,
            @RequestParam(value = "recipients", required = false, defaultValue = "[]") String recipientsJson) {

        SolicitudHr sol = new SolicitudHr();
        sol.setSectionId(sectionId);
        sol.setTitle(title);
        sol.setDescription(description);
        sol.setSenderEmail(senderEmail);
        sol.setSenderName(senderName);
        sol.setStatus("PENDIENTE");
        sol.setCreatedAt(LocalDateTime.now());
        sol.setUpdatedAt(LocalDateTime.now());

        if (file != null && !file.isEmpty()) {
            try {
                String originalName = file.getOriginalFilename();
                sol.setDocumentName(originalName);

                String excelUrl = cloudinaryService.uploadFile(file);
                sol.setDocumentUrl(excelUrl);

                try {
                    byte[] excelBytes = file.getBytes();
                    byte[] pdfBytes = excelToPdfService.convertExcelToPdf(excelBytes, originalName);

                    Optional<UserSignature> senderSig = sigRepo.findByEmail(senderEmail);
                    if (senderSig.isPresent() && senderSig.get().getSignatureUrl() != null && !senderSig.get().getSignatureUrl().isEmpty()) {
                        String senderRole = STAFF_ROLES.getOrDefault(senderEmail, "");
                        int totalSlots = 1;
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper2 = new com.fasterxml.jackson.databind.ObjectMapper();
                            List<Map<String, String>> rList = mapper2.readValue(recipientsJson,
                                    mapper2.getTypeFactory().constructCollectionType(List.class, Map.class));
                            totalSlots = 1 + rList.size();
                        } catch (Exception ignored) {}
                        pdfBytes = pdfService.stampSignatureOnPdf(pdfBytes, senderSig.get().getSignatureUrl(), senderName, senderRole, senderEmail, "ENVIADO", 0, totalSlots);
                    }

                    String b64 = Base64.getEncoder().encodeToString(pdfBytes);
                    sol.setPdfUrl("data:application/pdf;base64," + b64);
                } catch (Exception pdfEx) {
                    log.warn("Pre-conversion a PDF falló, se convertirá bajo demanda: {}", pdfEx.getMessage());
                }

            } catch (Exception e) {
                sol.setDocumentName(file.getOriginalFilename());
            }
        }

        sol = solRepo.save(sol);

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, String>> recipientsList = mapper.readValue(recipientsJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            int totalSlots = 1 + recipientsList.size();
            for (int i = 0; i < recipientsList.size(); i++) {
                Map<String, String> r = recipientsList.get(i);
                SolicitudHrRecipient recip = new SolicitudHrRecipient();
                recip.setSolicitudId(sol.getId());
                recip.setRecipientEmail(r.getOrDefault("email", ""));
                recip.setRecipientName(r.getOrDefault("name", ""));
                recip.setStatus(i == 0 ? "PENDIENTE" : "EN_ESPERA");
                recip.setOrderIndex(i);
                recip.setCreatedAt(LocalDateTime.now());
                recipRepo.save(recip);
            }
        } catch (Exception e) {
            log.warn("Error parsing recipients JSON: {}", e.getMessage());
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", sol.getId());
        resp.put("status", sol.getStatus());
        resp.put("message", "Solicitud enviada y ruta de aprobación configurada automáticamente");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/sent")
    public List<Map<String, Object>> getSent(@RequestParam String email) {
        List<SolicitudHr> sols = solRepo.findBySenderEmailOrderByCreatedAtDesc(email);
        return sols.stream().map(this::toMap).toList();
    }

    @GetMapping("/received")
    public List<Map<String, Object>> getReceived(@RequestParam String email) {
        List<SolicitudHrRecipient> recips = recipRepo.findByRecipientEmailOrderByCreatedAtDesc(email);
        Set<Long> solIds = new LinkedHashSet<>();
        for (SolicitudHrRecipient r : recips) {
            solIds.add(r.getSolicitudId());
        }
        List<SolicitudHr> senderSols = solRepo.findBySenderEmailOrderByCreatedAtDesc(email);
        for (SolicitudHr s : senderSols) {
            solIds.add(s.getId());
        }
        List<Long> allIds = new ArrayList<>(solIds);
        Map<Long, List<SolicitudHrRecipient>> recipMap = new HashMap<>();
        if (!allIds.isEmpty()) {
            recipRepo.findBySolicitudIds(allIds).forEach(r -> {
                recipMap.computeIfAbsent(r.getSolicitudId(), k -> new ArrayList<>()).add(r);
            });
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long solId : allIds) {
            solRepo.findById(solId).ifPresent(sol -> {
                Map<String, Object> m = toMap(sol);
                m.put("recipients", recipMap.getOrDefault(solId, List.of()).stream().map(this::toRecipMap).toList());
                m.put("isSender", email.equals(sol.getSenderEmail()));
                result.add(m);
            });
        }
        return result;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSolicitud(@PathVariable Long id) {
        return solRepo.findById(id)
                .map(sol -> {
                    Map<String, Object> m = toDetailMap(sol);
                    List<SolicitudHrRecipient> recips = recipRepo.findBySolicitudIdOrderByCreatedAtDesc(id);
                    m.put("recipients", recips.stream().map(this::toRecipMap).toList());
                    return ResponseEntity.ok(m);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getSolicitudPdf(@PathVariable Long id) {
        return buildPdfResponse(id, false);
    }

    @GetMapping("/{id}/pdf/download")
    public ResponseEntity<byte[]> downloadSolicitudPdf(@PathVariable Long id) {
        return buildPdfResponse(id, true);
    }

    private ResponseEntity<byte[]> buildPdfResponse(Long id, boolean forDownload) {
        Optional<SolicitudHr> opt = solRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SolicitudHr sol = opt.get();

        try {
            byte[] pdfBytes = null;

            if (sol.getPdfUrl() != null && !sol.getPdfUrl().isEmpty()) {
                if (sol.getPdfUrl().startsWith("data:")) {
                    String b64 = sol.getPdfUrl().substring(sol.getPdfUrl().indexOf(",") + 1);
                    pdfBytes = Base64.getDecoder().decode(b64);
                } else {
                    try (InputStream is = new URL(sol.getPdfUrl()).openStream()) {
                        pdfBytes = is.readAllBytes();
                    }
                }
            }

            if (pdfBytes == null || pdfBytes.length == 0) {
                String documentUrl = sol.getDocumentUrl();
                if (documentUrl == null || documentUrl.isBlank()) {
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
                }
                byte[] excelBytes;
                try (InputStream is = new URL(documentUrl).openStream()) {
                    excelBytes = is.readAllBytes();
                }
                String fileName = sol.getDocumentName() != null ? sol.getDocumentName() : "documento.xlsx";
                pdfBytes = excelToPdfService.convertExcelToPdf(excelBytes, fileName);

                String stampedB64 = Base64.getEncoder().encodeToString(pdfBytes);
                sol.setPdfUrl("data:application/pdf;base64," + stampedB64);
                solRepo.save(sol);
            }

            String fileName = sol.getDocumentName() != null ? sol.getDocumentName() : "documento.pdf";
            String pdfName = fileName.replaceAll("\\.[^.]+$", "") + ".pdf";

            ContentDisposition cd = forDownload
                    ? ContentDisposition.attachment().filename(pdfName).build()
                    : ContentDisposition.inline().filename(pdfName).build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(cd);
            headers.setContentLength(pdfBytes.length);
            headers.setCacheControl(CacheControl.noCache().cachePrivate());

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error al convertir solicitud {} a PDF: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error al convertir el documento: " + e.getMessage()).getBytes());
        }
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<?> respondToSolicitud(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String recipientEmail = (String) body.getOrDefault("recipientEmail", "");
        String responseText = (String) body.getOrDefault("response", "");
        String status = (String) body.getOrDefault("status", "APROBADO");

        List<SolicitudHrRecipient> recips = recipRepo.findBySolicitudIdAndRecipientEmail(id, recipientEmail);
        if (recips.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se encontró destinatario"));
        }

        SolicitudHrRecipient recip = recips.get(0);
        recip.setStatus(status);
        recip.setResponse(responseText);
        recip.setRespondedAt(LocalDateTime.now());
        recipRepo.save(recip);

        if ("APROBADO".equals(status) || "RECHAZADO".equals(status)) {
            solRepo.findById(id).ifPresent(sol -> {
                try {
                    Optional<UserSignature> sigOpt = sigRepo.findByEmail(recipientEmail);
                    String sigUrl = sigOpt.map(UserSignature::getSignatureUrl).orElse("");
                    String approverName = sigOpt.map(UserSignature::getName)
                            .orElse(STAFF_DIRECTORY.getOrDefault(recipientEmail, recip.getRecipientName()));
                    String approverRole = STAFF_ROLES.getOrDefault(recipientEmail, "Aprobador");

                    byte[] pdfBytes = null;
                    if (sol.getPdfUrl() != null && !sol.getPdfUrl().isEmpty()) {
                        if (sol.getPdfUrl().startsWith("data:")) {
                            String b64 = sol.getPdfUrl().substring(sol.getPdfUrl().indexOf(",") + 1);
                            pdfBytes = Base64.getDecoder().decode(b64);
                        } else {
                            try (InputStream is = new URL(sol.getPdfUrl()).openStream()) {
                                pdfBytes = is.readAllBytes();
                            }
                        }
                    }
                    if (pdfBytes == null && sol.getDocumentUrl() != null && !sol.getDocumentUrl().isEmpty()) {
                        try {
                            byte[] excelBytes;
                            try (InputStream is = new URL(sol.getDocumentUrl()).openStream()) {
                                excelBytes = is.readAllBytes();
                            }
                            String fileName = sol.getDocumentName() != null ? sol.getDocumentName() : "documento.xlsx";
                            pdfBytes = excelToPdfService.convertExcelToPdf(excelBytes, fileName);
                        } catch (Exception ignored) {}
                    }

                    if (pdfBytes != null && pdfBytes.length > 0) {
                        List<SolicitudHrRecipient> allRecips = recipRepo.findBySolicitudIdOrderByCreatedAtDesc(id);
                        int totalSlots = 1 + allRecips.size();
                        int slotIndex = recip.getOrderIndex() + 1;
                        byte[] stamped = pdfService.stampSignatureOnPdf(pdfBytes, sigUrl, approverName, approverRole, recipientEmail, status, slotIndex, totalSlots);
                        String stampedB64 = Base64.getEncoder().encodeToString(stamped);
                        sol.setPdfUrl("data:application/pdf;base64," + stampedB64);
                        sol.setUpdatedAt(LocalDateTime.now());
                        solRepo.save(sol);
                    }
                } catch (Exception e) {
                    // PDF stamping failed, continue with approval
                }
            });
        }

        solRepo.findById(id).ifPresent(sol -> {
            List<SolicitudHrRecipient> allRecips = recipRepo.findBySolicitudIdOrderByCreatedAtDesc(id);

            boolean allDone = allRecips.stream()
                    .allMatch(r -> "APROBADO".equals(r.getStatus()) || "RECHAZADO".equals(r.getStatus()));
            if (allDone) {
                boolean anyRejected = allRecips.stream().anyMatch(r -> "RECHAZADO".equals(r.getStatus()));
                sol.setStatus(anyRejected ? "RECHAZADO" : "PROCESADO");
            } else {
                sol.setStatus("EN_PROCESO");
                for (SolicitudHrRecipient r : allRecips) {
                    if ("EN_ESPERA".equals(r.getStatus())) {
                        boolean prevApproved = allRecips.stream()
                            .filter(p -> p.getOrderIndex() < r.getOrderIndex())
                            .allMatch(p -> "APROBADO".equals(p.getStatus()));
                        if (prevApproved) {
                            r.setStatus("PENDIENTE");
                            recipRepo.save(r);
                        }
                        break;
                    }
                }
            }
            sol.setUpdatedAt(LocalDateTime.now());
            solRepo.save(sol);
        });

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "APROBADO".equals(status) ? "Solicitud aprobada correctamente" : "Solicitud rechazada");
        resp.put("status", recip.getStatus());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/signature")
    public ResponseEntity<?> saveSignature(@RequestBody Map<String, Object> body) {
        String email = (String) body.getOrDefault("email", "");
        String name = (String) body.getOrDefault("name", "");
        String roleLabel = (String) body.getOrDefault("roleLabel", "");
        String signatureUrl = (String) body.getOrDefault("signatureUrl", "");

        if (email.isEmpty() || signatureUrl.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email y firma son requeridos"));
        }

        Optional<UserSignature> existing = sigRepo.findByEmail(email);
        UserSignature sig = existing.orElse(new UserSignature());
        sig.setEmail(email);
        sig.setName(name);
        sig.setRoleLabel(roleLabel);
        sig.setSignatureUrl(signatureUrl);
        sig.setUpdatedAt(LocalDateTime.now());
        sigRepo.save(sig);

        return ResponseEntity.ok(Map.of("message", "Firma guardada correctamente"));
    }

    @GetMapping("/signature/{email}")
    public ResponseEntity<?> getSignature(@PathVariable String email) {
        return sigRepo.findByEmail(email)
                .map(sig -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("email", sig.getEmail());
                    m.put("name", sig.getName());
                    m.put("roleLabel", sig.getRoleLabel());
                    m.put("signatureUrl", sig.getSignatureUrl());
                    return ResponseEntity.ok(m);
                })
                .orElse(ResponseEntity.ok(Map.of()));
    }

    private Map<String, Object> toMap(SolicitudHr sol) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", sol.getId());
        m.put("sectionId", sol.getSectionId());
        m.put("title", sol.getTitle());
        m.put("description", sol.getDescription());
        m.put("documentName", sol.getDocumentName());
        m.put("documentUrl", sol.getDocumentUrl());
        m.put("hasPdf", sol.getPdfUrl() != null && !sol.getPdfUrl().isEmpty());
        m.put("senderEmail", sol.getSenderEmail());
        m.put("senderName", sol.getSenderName());
        m.put("status", sol.getStatus());
        m.put("createdAt", sol.getCreatedAt() != null ? sol.getCreatedAt().toString() : null);
        m.put("updatedAt", sol.getUpdatedAt() != null ? sol.getUpdatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> toDetailMap(SolicitudHr sol) {
        Map<String, Object> m = toMap(sol);
        m.put("pdfUrl", sol.getPdfUrl());
        return m;
    }

    private Map<String, Object> toRecipMap(SolicitudHrRecipient r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("recipientEmail", r.getRecipientEmail());
        m.put("recipientName", r.getRecipientName());
        m.put("status", r.getStatus());
        m.put("response", r.getResponse());
        m.put("orderIndex", r.getOrderIndex());
        m.put("respondedAt", r.getRespondedAt() != null ? r.getRespondedAt().toString() : null);
        m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        return m;
    }
}
