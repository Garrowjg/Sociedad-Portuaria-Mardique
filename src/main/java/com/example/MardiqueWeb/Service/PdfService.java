package com.example.MardiqueWeb.Service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class PdfService {

    public byte[] generatePaymentReceipt(Object payment) {
        try {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph("Comprobante de Pago",
                    com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16)));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Generado automáticamente por el sistema.",
                    com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 10)));
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    public byte[] excelToPdf(byte[] excelBytes, String fileName) {
        try (InputStream is = new ByteArrayInputStream(excelBytes);
             Workbook workbook = new XSSFWorkbook(is);
             ByteArrayOutputStream pdfOut = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, pdfOut);
            document.open();

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(
                    com.lowagie.text.FontFactory.HELVETICA_BOLD, 14, new Color(0x1B, 0x45, 0x62));
            com.lowagie.text.Font headerFont = com.lowagie.text.FontFactory.getFont(
                    com.lowagie.text.FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            com.lowagie.text.Font cellFont = com.lowagie.text.FontFactory.getFont(
                    com.lowagie.text.FontFactory.HELVETICA, 9, Color.BLACK);

            String cleanName = fileName.replaceAll("\\.[^.]+$", "").replace("_", " ");
            Paragraph title = new Paragraph(cleanName, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12);
            document.add(title);

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(s);
                if (workbook.getNumberOfSheets() > 1) {
                    Paragraph sheetTitle = new Paragraph("Hoja: " + sheet.getSheetName(),
                            com.lowagie.text.FontFactory.getFont(
                                    com.lowagie.text.FontFactory.HELVETICA_BOLD, 11, new Color(0x1B, 0x45, 0x62)));
                    sheetTitle.setSpacingBefore(8);
                    sheetTitle.setSpacingAfter(6);
                    document.add(sheetTitle);
                }

                if (sheet.getPhysicalNumberOfRows() == 0) continue;

                int maxCol = 0;
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    if (row.getLastCellNum() > maxCol) maxCol = row.getLastCellNum();
                }
                if (maxCol == 0) continue;

                PdfPTable table = new PdfPTable(maxCol);
                table.setWidthPercentage(100);
                table.setSpacingBefore(4);
                table.setSpacingAfter(8);

                for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                    org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                    if (row == null) continue;
                    for (int c = 0; c < maxCol; c++) {
                        org.apache.poi.ss.usermodel.Cell cell = row.getCell(c, org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        String val = getCellValue(cell);
                        PdfPCell pCell = new PdfPCell(new Phrase(val, cellFont));
                        pCell.setPadding(4);
                        pCell.setBorderWidth(0.5f);
                        pCell.setBorderColor(Color.LIGHT_GRAY);
                        if (i == 0) {
                            pCell.setBackgroundColor(new Color(27, 69, 98));
                            pCell.setPhrase(new Phrase(val, headerFont));
                        }
                        table.addCell(pCell);
                    }
                }
                document.add(table);
            }

            document.close();
            return pdfOut.toByteArray();

        } catch (Exception e) {
            return createFallbackPdf(excelBytes, fileName);
        }
    }

    public byte[] stampSignatureOnPdf(byte[] pdfBytes, String signatureImageUrl,
                                       String approverName, String approverRole,
                                       String approverEmail, String status,
                                       int slotIndex, int totalSlots) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(pdfBytes);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            boolean isSent = "ENVIADO".equals(status);
            boolean isApproved = "APROBADO".equals(status);

            PdfReader reader = new PdfReader(bais);
            PdfStamper stamper = new PdfStamper(reader, baos);
            PdfContentByte over = stamper.getOverContent(1);
            float pageWidth = reader.getPageSizeWithRotation(1).getWidth();

            float marginL = 50;
            float marginR = 50;
            float availableW = pageWidth - marginL - marginR;
            float colW = availableW / totalSlots;
            float x = marginL + (slotIndex * colW);
            float y = 30;
            float lineY = y + 60;

            over.setColorStroke(new Color(200, 210, 220));
            over.setLineWidth(0.5f);
            over.moveTo(x + 10, lineY);
            over.lineTo(x + colW - 10, lineY);
            over.stroke();

            if (signatureImageUrl != null && !signatureImageUrl.isEmpty()) {
                try {
                    Image sigImage = Image.getInstance(new URL(signatureImageUrl));
                    sigImage.scaleAbsolute(100, 30);
                    sigImage.setAbsolutePosition(x + (colW - 100) / 2, lineY + 8);
                    over.addImage(sigImage);
                } catch (Exception e) {
                    drawSlotName(over, x, colW, lineY + 18, approverName);
                }
            } else {
                drawSlotName(over, x, colW, lineY + 18, approverName);
            }

            String displayName = (approverName != null && !approverName.isEmpty()) ? approverName : "";
            String displayRole = (approverRole != null && !approverRole.isEmpty()) ? approverRole : status;

            com.lowagie.text.Font nameFont = com.lowagie.text.FontFactory.getFont(
                    com.lowagie.text.FontFactory.HELVETICA_BOLD, 9, new Color(27, 69, 98));
            over.beginText();
            over.setFontAndSize(nameFont.getBaseFont(), 9);
            over.setColorFill(new Color(27, 69, 98));
            over.showTextAligned(PdfContentByte.ALIGN_CENTER, displayName, x + colW / 2, lineY - 14, 0);

            com.lowagie.text.Font roleFont = com.lowagie.text.FontFactory.getFont(
                    com.lowagie.text.FontFactory.HELVETICA, 8, new Color(100, 116, 139));
            over.setFontAndSize(roleFont.getBaseFont(), 8);
            over.setColorFill(new Color(100, 116, 139));
            over.showTextAligned(PdfContentByte.ALIGN_CENTER, displayRole, x + colW / 2, lineY - 26, 0);

            over.endText();

            stamper.close();
            reader.close();
            return baos.toByteArray();

        } catch (Exception e) {
            return pdfBytes;
        }
    }

    private void drawSlotName(PdfContentByte over, float x, float colW, float y, String name) {
        com.lowagie.text.Font nameFont = com.lowagie.text.FontFactory.getFont(
                com.lowagie.text.FontFactory.HELVETICA_BOLD, 9, new Color(27, 69, 98));
        over.beginText();
        over.setFontAndSize(nameFont.getBaseFont(), 9);
        over.setColorFill(new Color(27, 69, 98));
        over.showTextAligned(PdfContentByte.ALIGN_CENTER, name != null ? name : "", x + colW / 2, y, 0);
        over.endText();
    }

    public String stampSignatureAndUpload(byte[] pdfBytes, String signatureImageUrl,
                                           String approverName, String approverRole,
                                           String approverEmail, String status,
                                           int slotIndex, int totalSlots) {
        byte[] stamped = stampSignatureOnPdf(pdfBytes, signatureImageUrl, approverName, approverRole, approverEmail, status, slotIndex, totalSlots);
        return Base64.getEncoder().encodeToString(stamped);
    }

    private byte[] createFallbackPdf(byte[] excelBytes, String fileName) {
        try {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph("Documento: " + fileName,
                    com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12)));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("El archivo Excel original está disponible para descarga.",
                    com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 10)));
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private String getCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); } catch (Exception e) {
                    try { yield String.valueOf(cell.getNumericCellValue()); } catch (Exception e2) { yield ""; }
                }
            }
            default -> "";
        };
    }
}
