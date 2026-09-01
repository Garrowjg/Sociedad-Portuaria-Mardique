package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.IntranetHrDocument;
import com.example.MardiqueWeb.Entity.IntranetHrSectionView;
import com.example.MardiqueWeb.Repository.IntranetHrDocumentRepository;
import com.example.MardiqueWeb.Repository.IntranetHrSectionViewRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/intranet/hr")
public class IntranetHrController {

    private static final Logger log = LoggerFactory.getLogger(IntranetHrController.class);
    private static final Map<Long, byte[]> THUMB_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> IMAGE_TYPES = Set.of("jpg", "jpeg", "png", "gif", "webp", "svg");
    private static final Set<String> EXCEL_TYPES = Set.of("xls", "xlsx", "csv");
    private static final int MAX_SHEET_ROWS = 40;
    private static final int MAX_SHEET_COLS = 26;
    private final RestTemplate restTemplate = new RestTemplate();

    private final IntranetHrSectionViewRepository viewRepo;
    private final IntranetHrDocumentRepository docRepo;

    public IntranetHrController(IntranetHrSectionViewRepository viewRepo,
                                IntranetHrDocumentRepository docRepo) {
        this.viewRepo = viewRepo;
        this.docRepo = docRepo;
    }

    public static final Map<String, String> SECTION_NAMES = Map.of(
        "reglamento", "Reglamento Interno de Trabajo",
        "permisos", "Permisos de Trabajo",
        "excusas", "Excusas por Inasistencia",
        "vacaciones", "Solicitud de Vacaciones",
        "dias-libres", "Solicitud de Días Libres",
        "compensacion-vacaciones", "Formato de Compensación de Vacaciones"
    );

    /* ── QR ── */

    @GetMapping("/{sectionId}/qr")
    public ResponseEntity<byte[]> sectionQr(@PathVariable String sectionId,
                                            jakarta.servlet.http.HttpServletRequest req) {
        String scheme = req.getScheme();
        String host = req.getServerName();
        int port = req.getServerPort();
        String ctx = req.getContextPath();
        String portStr = (scheme.equals("https") && port == 443) || (scheme.equals("http") && port == 80) ? "" : ":" + port;
        String url = scheme + "://" + host + portStr + ctx + "/intranet/autoservicio-th#" + sectionId;
        byte[] png = generateQrPng(url);
        if (png == null) return ResponseEntity.status(500).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDisposition(ContentDisposition.inline().filename("qr-" + sectionId + ".png").build());
        headers.setCacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(10)).getHeaderValue());
        return new ResponseEntity<>(png, headers, HttpStatus.OK);
    }

    /* ── Views tracking ── */

    @PostMapping("/{sectionId}/view")
    public ResponseEntity<?> recordView(@PathVariable String sectionId,
                                        @RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String name = body.getOrDefault("name", "").trim();
        if (email.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "email required"));
        IntranetHrSectionView view = new IntranetHrSectionView();
        view.setSectionId(sectionId);
        view.setViewerEmail(email);
        view.setViewerName(name.isEmpty() ? null : name);
        view.setViewedAt(LocalDateTime.now());
        viewRepo.save(view);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/{sectionId}/views")
    public ResponseEntity<?> getViews(@PathVariable String sectionId) {
        List<IntranetHrSectionView> views = viewRepo.findBySectionIdOrderByViewedAtDesc(sectionId);
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (IntranetHrSectionView v : views) {
            String key = v.getViewerEmail();
            grouped.computeIfAbsent(key, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("email", v.getViewerEmail());
                m.put("name", v.getViewerName());
                m.put("veces", 0);
                m.put("primeraVez", v.getViewedAt().toString());
                m.put("ultimaVez", v.getViewedAt().toString());
                return m;
            });
            Map<String, Object> m = grouped.get(key);
            m.put("veces", (int) m.get("veces") + 1);
            m.put("ultimaVez", v.getViewedAt().toString());
        }
        return ResponseEntity.ok(new ArrayList<>(grouped.values()));
    }

    @GetMapping("/views/events")
    public ResponseEntity<?> allEvents() {
        return ResponseEntity.ok(viewRepo.findAll());
    }

    @GetMapping("/views/stats")
    public ResponseEntity<?> stats() {
        List<IntranetHrSectionView> all = viewRepo.findAll();
        Map<String, Long> bySection = all.stream()
            .collect(Collectors.groupingBy(IntranetHrSectionView::getSectionId, Collectors.counting()));
        long totalViews = all.size();
        Set<String> uniqueEmails = all.stream().map(IntranetHrSectionView::getViewerEmail).collect(Collectors.toSet());
        return ResponseEntity.ok(Map.of(
            "totalViews", totalViews,
            "uniqueUsers", uniqueEmails.size(),
            "bySection", bySection
        ));
    }

    /* ── Documents CRUD ── */

    @GetMapping("/{sectionId}/docs")
    public ResponseEntity<?> listDocs(@PathVariable String sectionId) {
        return ResponseEntity.ok(docRepo.findBySectionIdOrderByUploadedAtDesc(sectionId));
    }

    @PostMapping("/{sectionId}/docs")
    public ResponseEntity<?> addDoc(@PathVariable String sectionId, @RequestBody Map<String, String> body) {
        String fileName = body.getOrDefault("fileName", "").trim();
        String fileUrl = body.getOrDefault("fileUrl", "").trim();
        String description = body.getOrDefault("description", "").trim();
        String uploadedBy = body.getOrDefault("uploadedBy", "").trim();
        if (fileName.isEmpty() || fileUrl.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "fileName and fileUrl required"));
        }
        IntranetHrDocument doc = new IntranetHrDocument();
        doc.setSectionId(sectionId);
        doc.setFileName(fileName);
        doc.setFileUrl(fileUrl);
        doc.setDescription(description.isEmpty() ? null : description);
        doc.setUploadedBy(uploadedBy.isEmpty() ? null : uploadedBy);
        doc.setUploadedAt(LocalDateTime.now());
        docRepo.save(doc);
        return ResponseEntity.ok(doc);
    }

    @DeleteMapping("/docs/{docId}")
    public ResponseEntity<?> deleteDoc(@PathVariable Long docId) {
        docRepo.deleteById(docId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /* ── Thumbnail ── */

    @GetMapping("/docs/{docId}/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@PathVariable Long docId) {
        try {
            Optional<IntranetHrDocument> opt = docRepo.findById(docId);
            if (opt.isEmpty()) return ResponseEntity.notFound().build();
            IntranetHrDocument doc = opt.get();
            byte[] cached = THUMB_CACHE.get(docId);
            if (cached != null) {
                return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(cached);
            }
            String ext = getExt(doc.getFileName());
            byte[] png;
            if ("pdf".equals(ext)) {
                png = pdfThumb(doc.getFileUrl());
            } else if (IMAGE_TYPES.contains(ext)) {
                png = imageThumb(doc.getFileUrl());
            } else if (EXCEL_TYPES.contains(ext)) {
                png = excelThumb(doc.getFileUrl(), ext);
            } else {
                png = genericThumb(ext);
            }
            if (png != null) THUMB_CACHE.put(docId, png);
            if (png == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
        } catch (Exception e) {
            log.error("HR thumbnail error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    private String getExt(String fileName) {
        if (fileName == null) return "";
        int i = fileName.lastIndexOf('.');
        return i >= 0 ? fileName.substring(i + 1).toLowerCase() : "";
    }

    private byte[] fetchBytes(String url) {
        return restTemplate.getForObject(url, byte[].class);
    }

    private byte[] pdfThumb(String fileUrl) {
        try {
            byte[] pdfBytes = fetchBytes(fileUrl);
            if (pdfBytes == null) return genericThumb("pdf");
            try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
                if (pdf.getNumberOfPages() == 0) return genericThumb("pdf");
                PDFRenderer renderer = new PDFRenderer(pdf);
                BufferedImage page = renderer.renderImageWithDPI(0, 110);
                return toPng(scaleToFit(page, 420, 560));
            }
        } catch (Exception e) {
            log.warn("PDF thumb failed, fallback generic: {}", e.getMessage());
            return genericThumb("pdf");
        }
    }

    private byte[] imageThumb(String fileUrl) {
        try {
            byte[] imgBytes = fetchBytes(fileUrl);
            if (imgBytes == null) return genericThumb("img");
            BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(imgBytes));
            if (img == null) return genericThumb("img");
            return toPng(scaleToFit(img, 420, 560));
        } catch (Exception e) {
            return genericThumb("img");
        }
    }

    private byte[] excelThumb(String fileUrl, String ext) {
        try {
            byte[] data = fetchBytes(fileUrl);
            if (data == null) return genericThumb(ext);
            String[][] grid = parseSpreadsheet(data, ext);
            if (grid == null) return genericThumb(ext);
            return toPng(drawSheetGrid(grid, ext));
        } catch (Exception e) {
            return genericThumb(ext);
        }
    }

    private String[][] parseSpreadsheet(byte[] data, String ext) {
        try (Workbook wb = WorkbookFactory.create(new java.io.ByteArrayInputStream(data))) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            String[][] grid = new String[MAX_SHEET_ROWS][MAX_SHEET_COLS];
            for (String[] row : grid) Arrays.fill(row, "");
            int maxRow = Math.min(sheet.getLastRowNum() + 1, MAX_SHEET_ROWS);
            for (int r = 0; r < maxRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                int last = Math.min(Math.max(row.getLastCellNum(), 0), MAX_SHEET_COLS);
                for (int col = 0; col < last; col++) {
                    Cell cell = row.getCell(col);
                    if (cell != null) grid[r][col] = fmt.formatCellValue(cell);
                }
            }
            return grid;
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage drawSheetGrid(String[][] grid, String ext) {
        int w = 420, h = 560;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setColor(typeColor(ext));
            g.fillRect(0, 0, w, 8);
            int margin = 18, top = 22, rowH = 16;
            int usedCols = MAX_SHEET_COLS;
            outer:
            for (int colC = MAX_SHEET_COLS - 1; colC >= 0; colC--) {
                for (int r = 0; r < grid.length; r++) {
                    if (!grid[r][colC].isEmpty()) { usedCols = colC + 1; break outer; }
                }
            }
            if (usedCols < 1) usedCols = 1;
            int colW = (w - 2 * margin) / usedCols;
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            g.setColor(new Color(240, 242, 245));
            g.fillRect(margin, top, w - 2 * margin, rowH);
            for (int col = 0; col < usedCols; col++) {
                int x = margin + col * colW;
                g.setColor(new Color(60, 64, 67));
                g.drawString(String.valueOf((char) ('A' + col)), x + 4, top + 12);
                g.setColor(new Color(226, 230, 235));
                g.drawLine(x, top, x, top + rowH);
            }
            g.drawLine(margin + w - 2 * margin, top, margin + w - 2 * margin, top + rowH);
            g.drawLine(margin, top + rowH, margin + w - 2 * margin, top + rowH);
            int y = top + rowH;
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            for (int r = 0; r < grid.length && y + rowH <= h - 10; r++) {
                g.setColor(r % 2 == 0 ? Color.WHITE : new Color(248, 249, 251));
                g.fillRect(margin, y, w - 2 * margin, rowH);
                for (int col = 0; col < usedCols; col++) {
                    int x = margin + col * colW;
                    String cell = grid[r][col];
                    if (!cell.isEmpty()) {
                        g.setColor(new Color(60, 64, 67));
                        String draw = cell.length() > 14 ? cell.substring(0, 13) + "…" : cell;
                        g.drawString(draw, x + 4, y + 12);
                    }
                    g.setColor(new Color(226, 230, 235));
                    g.drawLine(x, y, x, y + rowH);
                }
                g.setColor(new Color(226, 230, 235));
                g.drawLine(margin + w - 2 * margin, y, margin + w - 2 * margin, y + rowH);
                g.drawLine(margin, y + rowH, margin + w - 2 * margin, y + rowH);
                y += rowH;
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    private byte[] genericThumb(String ext) {
        int w = 420, h = 560;
        try {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            Color c = typeColor(ext);
            g.setColor(c);
            g.fillRect(0, 0, w, 14);
            g.fillRoundRect(50, 80, w - 100, 280, 16, 16);
            g.dispose();
            return toPng(img);
        } catch (Exception e) {
            return null;
        }
    }

    private Color typeColor(String ext) {
        if (ext == null) return new Color(148, 163, 184);
        return switch (ext.toLowerCase()) {
            case "pdf" -> new Color(220, 38, 38);
            case "doc", "docx" -> new Color(37, 99, 235);
            case "xls", "xlsx", "csv" -> new Color(22, 163, 74);
            case "jpg", "jpeg", "png", "gif", "webp", "svg" -> new Color(168, 85, 247);
            default -> new Color(148, 163, 184);
        };
    }

    private BufferedImage scaleToFit(BufferedImage src, int maxW, int maxH) {
        double scale = Math.min((double) maxW / src.getWidth(), (double) maxH / src.getHeight());
        int nw = (int) (src.getWidth() * scale);
        int nh = (int) (src.getHeight() * scale);
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    private byte[] toPng(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    /* ── QR generation ── */

    private byte[] generateQrPng(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            Map<com.google.zxing.EncodeHintType, Object> hints = new HashMap<>();
            hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(com.google.zxing.EncodeHintType.MARGIN, 2);
            com.google.zxing.qrcode.QRCodeWriter writer = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix matrix = writer.encode(url,
                    com.google.zxing.BarcodeFormat.QR_CODE, 600, 600, hints);
            int w = matrix.getWidth();
            int h = matrix.getHeight();
            int scale = 4;
            BufferedImage img = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, img.getWidth(), img.getHeight());
                g.setColor(new Color(2, 6, 23));
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        if (matrix.get(x, y)) {
                            g.fillRect(x * scale, y * scale, scale, scale);
                        }
                    }
                }
            } finally {
                g.dispose();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
