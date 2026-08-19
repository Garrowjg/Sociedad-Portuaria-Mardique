package com.example.MardiqueWeb.Service;

import com.example.MardiqueWeb.Entity.IntranetDocument;
import com.example.MardiqueWeb.Repository.IntranetDocumentRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IntranetDocumentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "csv", "txt", "jpg", "jpeg", "png", "gif", "webp", "svg",
            "zip", "rar", "7z", "mp4");

    private static final Set<String> IMAGE_TYPES = Set.of("jpg", "jpeg", "png", "gif", "webp", "svg");
    private static final java.util.Map<Long, byte[]> THUMB_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("csv", "text/csv"),
            Map.entry("txt", "text/plain"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("zip", "application/zip"),
            Map.entry("mp4", "video/mp4"));

    private final IntranetDocumentRepository repository;
    private final Path uploadDir;

    public IntranetDocumentService(IntranetDocumentRepository repository,
                                   @Value("${intranet.upload-dir:uploads/intranet}") String uploadDir) {
        this.repository = repository;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el directorio de subida", e);
        }
    }

    /** Sanitiza el sector para usarlo solo como metadato/log; no se usa como ruta. */
    public String sanitizeSector(String sector) {
        if (sector == null || sector.isBlank()) return "general";
        String s = sector.trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
        return s.isEmpty() ? "general" : s;
    }

    public IntranetDocument upload(String sector, MultipartFile file, String uploadedBy) throws IOException {
        return upload(sector, file, uploadedBy, null);
    }

    public IntranetDocument upload(String sector, MultipartFile file, String uploadedBy, Long parentId) throws IOException {
        String original = file.getOriginalFilename();
        String ext = extension(original);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido: ." + ext);
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío.");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = uploadDir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        IntranetDocument doc = new IntranetDocument();
        doc.setSector(sanitizeSector(sector));
        doc.setNombre(original);
        doc.setStoredName(storedName);
        doc.setFileType(ext);
        doc.setFileSize(file.getSize());
        doc.setUploadedBy(uploadedBy == null || uploadedBy.isBlank() ? "Equipo Mardique" : uploadedBy.trim());
        doc.setUploadedAt(LocalDateTime.now());
        doc.setEsCarpeta(false);
        doc.setParentId(validateParent(parentId));
        return repository.save(doc);
    }

    /** Crea una carpeta vacía en un sector (o dentro de otra carpeta). */
    public IntranetDocument createFolder(String sector, String nombre, String uploadedBy, Long parentId) {
        String name = nombre == null ? "" : nombre.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("El nombre de la carpeta no puede estar vacío.");
        }
        IntranetDocument folder = new IntranetDocument();
        folder.setSector(sanitizeSector(sector));
        folder.setNombre(name);
        folder.setStoredName(null);
        folder.setFileType("folder");
        folder.setFileSize(0);
        folder.setUploadedBy(uploadedBy == null || uploadedBy.isBlank() ? "Equipo Mardique" : uploadedBy.trim());
        folder.setUploadedAt(LocalDateTime.now());
        folder.setEsCarpeta(true);
        folder.setParentId(validateParent(parentId));
        return repository.save(folder);
    }

    /** Valida que el parentId (si viene) corresponda a una carpeta existente y del mismo sector. */
    private Long validateParent(Long parentId) {
        if (parentId == null) return null;
        IntranetDocument parent = repository.findById(parentId).orElse(null);
        if (parent == null || !parent.isEsCarpeta()) {
            throw new IllegalArgumentException("La carpeta destino no existe.");
        }
        return parentId;
    }

    /** Lista la raíz de un sector (carpetas y archivos sin padre). */
    public List<IntranetDocument> listBySector(String sector) {
        return repository.findBySectorAndParentIdIsNullOrderByUploadedAtDesc(sanitizeSector(sector));
    }

    /** Lista el contenido de una carpeta. */
    public List<IntranetDocument> listByParent(Long parentId) {
        return repository.findByParentIdOrderByUploadedAtDesc(parentId);
    }

    /** Lista todos los documentos de un sector (raíz + contenido de carpetas), para conteos. */
    public List<IntranetDocument> listAllBySector(String sector) {
        return repository.findBySector(sanitizeSector(sector));
    }

    public long countChildren(Long parentId) {
        return repository.countByParentId(parentId);
    }

    public IntranetDocument find(Long id) {
        return repository.findById(id).orElse(null);
    }

    public boolean delete(Long id) throws IOException {
        IntranetDocument doc = repository.findById(id).orElse(null);
        if (doc == null) return false;
        deleteRecursive(doc);
        return true;
    }

    private void deleteRecursive(IntranetDocument doc) throws IOException {
        if (doc.isEsCarpeta()) {
            for (IntranetDocument child : repository.findByParentId(doc.getId())) {
                deleteRecursive(child);
            }
        } else {
            Path target = uploadDir.resolve(doc.getStoredName()).normalize();
            if (target.startsWith(uploadDir)) {
                Files.deleteIfExists(target);
            }
            Files.deleteIfExists(uploadDir.resolve(doc.getStoredName() + ".pdf"));
            THUMB_CACHE.remove(doc.getId());
        }
        repository.deleteById(doc.getId());
    }

    public byte[] readContent(IntranetDocument doc) throws IOException {
        Path target = uploadDir.resolve(doc.getStoredName()).normalize();
        if (!target.startsWith(uploadDir) || !Files.exists(target)) {
            throw new IOException("Archivo no encontrado.");
        }
        return Files.readAllBytes(target);
    }

    public String contentType(String ext) {
        return CONTENT_TYPES.getOrDefault(ext == null ? "" : ext.toLowerCase(), "application/octet-stream");
    }

    /* ---------- Código QR ---------- */

    /**
     * Genera una imagen PNG con el código QR que apunta a la URL de descarga del
     * documento. Si falla la generación, devuelve null.
     */
    public byte[] qrPng(IntranetDocument doc, String url) {
        if (url == null || url.isBlank()) return null;
        try {
            Map<com.google.zxing.EncodeHintType, Object> hints = new java.util.HashMap<>();
            hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(com.google.zxing.EncodeHintType.MARGIN, 2);
            com.google.zxing.qrcode.QRCodeWriter writer = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix matrix = writer.encode(url,
                    com.google.zxing.BarcodeFormat.QR_CODE, 600, 600, hints);
            return toPng(matrixToImage(matrix, Color.WHITE, new Color(2, 6, 23)));
        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage matrixToImage(com.google.zxing.common.BitMatrix matrix,
                                               Color onColor, Color offColor) {
        int w = matrix.getWidth();
        int h = matrix.getHeight();
        int scale = 4;
        BufferedImage img = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(offColor);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            g.setColor(onColor);
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
        return img;
    }

    public boolean isPreviewable(IntranetDocument doc) {
        return "pdf".equals(doc.getFileType())
                || Set.of("jpg", "jpeg", "png", "gif", "webp", "svg").contains(doc.getFileType());
    }

    /* ---------- Miniaturas (PNG) ---------- */

    public byte[] thumbnail(IntranetDocument doc) throws IOException {
        byte[] cached = THUMB_CACHE.get(doc.getId());
        if (cached != null) return cached;
        byte[] png;
        String t = doc.getFileType();
        try {
            if ("pdf".equals(t)) {
                png = pdfThumb(doc);
            } else if (IMAGE_TYPES.contains(t)) {
                png = imageThumb(doc);
            } else if ("txt".equals(t) || "csv".equals(t)) {
                png = textThumb(doc);
            } else if (isOfficeType(t)) {
                png = officeThumb(doc);
            } else {
                png = genericThumb(doc);
            }
        } catch (Throwable e) {
            // Cualquier fallo (p.ej. falta de freetype/fontconfig para AWT) no debe romper
            // la petición: se devuelve una miniatura simple dibujada sin texto.
            png = simpleFallbackThumb(doc.getFileType());
        }
        THUMB_CACHE.put(doc.getId(), png);
        return png;
    }

    /** Miniatura de emergencia: dibujada con formas/colores pero SIN texto (no usa fuentes). */
    private static byte[] simpleFallbackThumb(String ext) {
        int w = 420, h = 560;
        try {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setColor(fileTypeColorSafe(ext));
            g.fillRect(0, 0, w, 12);
            g.fillRoundRect(40, 60, 340, 300, 14, 14);
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private static Color fileTypeColorSafe(String ext) {
        if (ext == null) return new Color(148, 163, 184);
        switch (ext.toLowerCase()) {
            case "pdf": return new Color(220, 38, 38);
            case "doc": case "docx": return new Color(37, 99, 235);
            case "xls": case "xlsx": case "csv": return new Color(22, 163, 74);
            case "ppt": case "pptx": return new Color(234, 88, 12);
            case "jpg": case "jpeg": case "png": case "gif": case "webp": case "svg": return new Color(168, 85, 247);
            case "txt": return new Color(100, 116, 139);
            default: return new Color(148, 163, 184);
        }
    }

    private byte[] pdfThumb(IntranetDocument doc) throws IOException {
        byte[] png = renderFirstPagePng(readContent(doc));
        return png != null ? png : genericThumb(doc);
    }

    /** Renderiza la primera página de un PDF (bytes) como PNG, o null si falla. */
    private byte[] renderFirstPagePng(byte[] pdfData) {
        try (PDDocument pdf = Loader.loadPDF(pdfData)) {
            if (pdf.getNumberOfPages() == 0) return null;
            PDFRenderer renderer = new PDFRenderer(pdf);
            BufferedImage page = renderer.renderImageWithDPI(0, 110);
            return toPng(scaledToFit(page, 420, 560));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Miniatura "estilo Google Drive" para Word/Excel/PowerPoint: convierte el
     * documento a PDF con LibreOffice headless y renderiza la primera página real,
     * con el formato/diseño original. Si no hay LibreOffice, cae al renderizado
     * previo (grilla de Excel o página genérica).
     */
    private byte[] officeThumb(IntranetDocument doc) throws IOException {
        byte[] pdfBytes = previewPdf(doc);
        if (pdfBytes != null) {
            byte[] png = renderFirstPagePng(pdfBytes);
            if (png != null) return png;
        }
        if ("xls".equals(doc.getFileType()) || "xlsx".equals(doc.getFileType())) {
            return spreadsheetThumb(doc);
        }
        return genericThumb(doc);
    }

    /** ¿El tipo de archivo se puede convertir a PDF con LibreOffice? */
    private static boolean isOfficeType(String t) {
        return "doc".equals(t) || "docx".equals(t)
                || "xls".equals(t) || "xlsx".equals(t)
                || "ppt".equals(t) || "pptx".equals(t);
    }

    /**
     * Devuelve el PDF de un documento: el original si ya es PDF, o el convertido
     * con LibreOffice para Word/Excel/PowerPoint. El PDF convertido se guarda en
     * disco junto al archivo original para no reconvertir cada vez. Devuelve null
     * si no aplica o no se pudo convertir.
     */
    public byte[] previewPdf(IntranetDocument doc) throws IOException {
        String t = doc.getFileType();
        if ("pdf".equals(t)) return readContent(doc);
        if (!isOfficeType(t)) return null;
        Path cached = uploadDir.resolve(doc.getStoredName() + ".pdf");
        if (Files.exists(cached)) return Files.readAllBytes(cached);
        byte[] pdf = convertToPdf(readContent(doc), t);
        if (pdf != null) {
            try {
                Files.write(cached, pdf);
            } catch (IOException ignored) {
            }
        }
        return pdf;
    }

    /** Convierte bytes de un documento de Office a PDF usando `soffice --headless`. Devuelve null si falla. */
    private byte[] convertToPdf(byte[] data, String ext) {
        Path tmpDir = null;
        try {
            tmpDir = Files.createTempDirectory("office-thumb-");
            Path input = tmpDir.resolve("input." + ext);
            Files.write(input, data);

            String[] cmd = sofficeCommand();
            if (cmd == null) return null;
            List<String> args = new ArrayList<>();
            args.addAll(Arrays.asList(cmd));
            args.addAll(Arrays.asList("--headless", "--norestore", "--nolockcheck",
                    "--convert-to", "pdf", "--outdir", tmpDir.toString(), input.toString()));

            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (var is = process.getInputStream()) {
                is.readAllBytes();
            }
            boolean finished = process.waitFor(25, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return null;
            }
            Path output = tmpDir.resolve("input.pdf");
            if (!Files.exists(output)) return null;
            return Files.readAllBytes(output);
        } catch (Exception e) {
            return null;
        } finally {
            if (tmpDir != null) {
                try {
                    Files.walk(tmpDir).sorted(java.util.Comparator.reverseOrder())
                            .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
                } catch (IOException ignored) {
                }
            }
        }
    }

    /** Devuelve el comando de LibreOffice (soffice), o null si no está instalado. */
    private String[] sofficeCommand() {
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> candidates = new ArrayList<>();
        if (os.contains("win")) {
            candidates.add("C:\\Program Files\\LibreOffice\\program\\soffice.exe");
            candidates.add("C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe");
        } else {
            candidates.add("/usr/bin/soffice");
            candidates.add("/usr/bin/libreoffice");
            candidates.add("/snap/bin/soffice");
        }
        for (String c : candidates) {
            try {
                if (Files.exists(Paths.get(c))) return new String[]{c};
            } catch (Exception ignored) {
            }
        }
        for (String c : new String[]{"soffice", "libreoffice"}) {
            try {
                Process probe = new ProcessBuilder(c, "--version").start();
                probe.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                return new String[]{c};
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private byte[] imageThumb(IntranetDocument doc) throws IOException {
        byte[] data = readContent(doc);
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            if (img == null) return genericThumb(doc);
            return toPng(scaledToFit(img, 420, 560));
        } catch (Exception e) {
            return genericThumb(doc);
        }
    }

    private byte[] textThumb(IntranetDocument doc) throws IOException {
        byte[] data = readContent(doc);
        String text = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        if (text.length() > 3000) text = text.substring(0, 3000);
        BufferedImage page = drawTextPage(text, doc.getFileType());
        return toPng(page);
    }

    private byte[] genericThumb(IntranetDocument doc) throws IOException {
        int w = 360, h = 480;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Página blanca
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            // Franja de color del tipo de archivo
            Color c = fileTypeColor(doc.getFileType());
            g.setColor(c);
            g.fillRect(0, 0, w, 10);
            // Bloque tipo documento
            g.setColor(c);
            g.fillRoundRect(40, 40, 34, 40, 8, 8);
            g.setColor(Color.WHITE);
            Font extFont = new Font(Font.SANS_SERIF, Font.BOLD, 13);
            g.setFont(extFont);
            String ext = doc.getFileType().toUpperCase();
            FontMetrics fm = g.getFontMetrics();
            int extW = fm.stringWidth(ext);
            g.drawString(ext, 40 + (34 - extW) / 2, 40 + 26);
            // Nombre
            g.setColor(Color.WHITE);
            g.fillRoundRect(84, 44, w - 84 - 40, 30, 8, 8);
            g.setColor(c);
            String name = doc.getNombre() == null ? "" : doc.getNombre();
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            g.drawString(truncate(g, name, w - 84 - 40 - 16), 96, 64);
            // Líneas simulando texto
            g.setColor(new Color(230, 233, 237));
            int y = 130;
            int[] widths = {220, 260, 240, 200, 260, 210, 240, 230, 190};
            for (int i = 0; i < widths.length; i++) {
                g.fillRoundRect(40, y, widths[i], 9, 4, 4);
                y += 22;
            }
            // Nombre al pie
            g.setColor(new Color(120, 128, 140));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            String footer = truncate(g, doc.getNombre() == null ? "" : doc.getNombre(), w - 80);
            g.drawString(footer, (w - g.getFontMetrics().stringWidth(footer)) / 2, h - 28);
        } finally {
            g.dispose();
        }
        return toPng(img);
    }

    private byte[] spreadsheetThumb(IntranetDocument doc) throws IOException {
        try {
            String[][] grid = parseSpreadsheet(readContent(doc));
            if (grid == null) return genericThumb(doc);
            return toPng(drawSheetGrid(grid, doc.getFileType()));
        } catch (Exception e) {
            return genericThumb(doc);
        }
    }

    /** Devuelve la hoja de cálculo como filas de celdas (texto), recortadas a 40x14. */
    public List<List<String>> sheetData(IntranetDocument doc) {
        if (!"xlsx".equals(doc.getFileType()) && !"xls".equals(doc.getFileType())) return null;
        try {
            String[][] grid = parseSpreadsheet(readContent(doc));
            if (grid == null) return null;
            int maxCol = 0, maxRow = 0;
            for (int r = 0; r < grid.length; r++) {
                for (int col = 0; col < grid[r].length; col++) {
                    if (!grid[r][col].isEmpty()) {
                        if (col + 1 > maxCol) maxCol = col + 1;
                        if (r + 1 > maxRow) maxRow = r + 1;
                    }
                }
            }
            if (maxRow == 0 || maxCol == 0) return List.of();
            List<List<String>> rows = new ArrayList<>();
            for (int r = 0; r < maxRow; r++) {
                List<String> row = new ArrayList<>();
                for (int col = 0; col < maxCol; col++) row.add(grid[r][col]);
                rows.add(row);
            }
            return rows;
        } catch (Exception e) {
            return null;
        }
    }

    private static final int MAX_SHEET_ROWS = 40;
    private static final int MAX_SHEET_COLS = 26;

    /** Lee un libro Excel (.xls o .xlsx) con Apache POI y devuelve una cuadrícula de celdas, o null si no se puede. */
    private String[][] parseSpreadsheet(byte[] data) {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(data))) {
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
                    if (cell != null) {
                        grid[r][col] = fmt.formatCellValue(cell);
                    }
                }
            }
            return grid;
        } catch (Exception e) {
            return null;
        }
    }

    /** Genera una página HTML completa con la hoja de cálculo estilizada (para mostrar en iframe). */
    public String toHtml(IntranetDocument doc) {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(readContent(doc)))) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            int lastCol = 0;
            for (Row row : sheet) {
                if (row != null && row.getLastCellNum() > lastCol) lastCol = row.getLastCellNum();
            }
            if (lastCol < 1) lastCol = 1;
            StringBuilder h = new StringBuilder();
            h.append("<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'><style>");
            h.append("body{margin:0;padding:24px 28px;font-family:Arial,Helvetica,sans-serif;background:#fff;color:#1a1a1a;}");
            h.append("table{border-collapse:collapse;width:100%;font-size:13px;}");
            h.append("th,td{border:1px solid #d0d5dd;padding:5px 10px;text-align:left;vertical-align:top;}");
            h.append("th{background:#f1f3f5;font-weight:700;position:sticky;top:0;z-index:1;}");
            h.append("thead th{background:#e8ecf0;}");
            h.append("td{white-space:nowrap;max-width:320px;overflow:hidden;text-overflow:ellipsis;}");
            h.append("tr:nth-child(even) td{background:#fafbfc;}");
            h.append("th.rh{background:#f1f3f5;color:#888;text-align:center;width:48px;position:sticky;left:0;z-index:2;}");
            h.append("th.corner{background:#e8ecf0;position:sticky;left:0;z-index:3;}");
            h.append("</style></head><body><table><thead><tr><th class='corner'></th>");
            for (int c = 0; c < lastCol; c++) h.append("<th>").append(sheetColName(c)).append("</th>");
            h.append("</tr></thead><tbody>");
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                h.append("<tr><th class='rh'>").append(r + 1).append("</th>");
                Row row = sheet.getRow(r);
                for (int c = 0; c < lastCol; c++) {
                    h.append("<td>");
                    if (row != null) {
                        Cell cell = row.getCell(c);
                        if (cell != null) h.append(escapeHtml(fmt.formatCellValue(cell)));
                    }
                    h.append("</td>");
                }
                h.append("</tr>");
            }
            h.append("</tbody></table></body></html>");
            return h.toString();
        } catch (Exception e) {
            return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='display:flex;align-items:center;justify-content:center;height:100vh;color:#999;font-family:sans-serif;'>No se pudo leer la hoja de cálculo.</body></html>";
        }
    }

    private static String sheetColName(int i) {
        String name = "";
        int n = i;
        do { name = (char) ('A' + n % 26) + name; n = n / 26 - 1; } while (n >= 0);
        return name;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private BufferedImage drawSheetGrid(String[][] grid, String fileType) {
        int w = 420, h = 560;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            Color c = fileTypeColor(fileType);
            g.setColor(c);
            g.fillRect(0, 0, w, 8);

int margin = 18;
        int top = 22;
        int rowH = 16;
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
        FontMetrics fm = g.getFontMetrics();

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
            g.setColor(r % 2 == 0 ? new Color(255, 255, 255) : new Color(248, 249, 251));
            g.fillRect(margin, y, w - 2 * margin, rowH);
            for (int col = 0; col < usedCols; col++) {
                    int x = margin + col * colW;
                    String cell = grid[r][col];
                    if (!cell.isEmpty()) {
                        g.setColor(new Color(60, 64, 67));
                        g.drawString(truncate(g, cell, colW - 6), x + 4, y + 12);
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

    private BufferedImage drawTextPage(String text, String fileType) {
        int w = 420, h = 560;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            Color c = fileTypeColor(fileType);
            g.setColor(c);
            g.fillRect(0, 0, w, 8);
            g.setColor(new Color(60, 64, 67));
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
            int margin = 24, y = 40, lineHeight = 22;
            FontMetrics fm = g.getFontMetrics();
            int maxW = w - 2 * margin;
            for (String rawLine : text.split("\\R")) {
                String line = rawLine.trim();
                if (line.isEmpty()) { y += lineHeight; continue; }
                while (line.length() > 0 && y < h - 20) {
                    String chunk = line;
                    if (fm.stringWidth(chunk) > maxW) {
                        int cut = line.length();
                        while (cut > 1 && fm.stringWidth(line.substring(0, cut)) > maxW) cut--;
                        chunk = line.substring(0, cut);
                    }
                    g.setColor(new Color(60, 64, 67));
                    g.drawString(chunk, margin, y);
                    y += lineHeight;
                    line = line.length() > chunk.length() ? line.substring(chunk.length()).trim() : "";
                }
                if (y >= h - 20) break;
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    private Color fileTypeColor(String ext) {
        switch (ext == null ? "" : ext.toLowerCase()) {
            case "pdf": return new Color(220, 38, 38);
            case "doc": case "docx": return new Color(37, 99, 235);
            case "xls": case "xlsx": case "csv": return new Color(22, 163, 74);
            case "ppt": case "pptx": return new Color(234, 88, 12);
            case "jpg": case "jpeg": case "png": case "gif": case "webp": case "svg": return new Color(168, 85, 247);
            case "txt": return new Color(100, 116, 139);
            default: return new Color(148, 163, 184);
        }
    }

    private BufferedImage scaledToFit(BufferedImage src, int maxW, int maxH) {
        double scale = Math.min(1.0, Math.min((double) maxW / src.getWidth(), (double) maxH / src.getHeight()));
        int nw = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int nh = Math.max(1, (int) Math.round(src.getHeight() * scale));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private byte[] toPng(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private String truncate(Graphics2D g, String s, int maxPx) {
        if (s == null) return "";
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(s) <= maxPx) return s;
        while (s.length() > 1 && fm.stringWidth(s + "…") > maxPx) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private static String extension(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        if (i < 0 || i == name.length() - 1) return "";
        return name.substring(i + 1).toLowerCase();
    }
}