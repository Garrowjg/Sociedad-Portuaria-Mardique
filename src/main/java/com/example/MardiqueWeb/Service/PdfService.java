package com.example.MardiqueWeb.Service;

import com.example.MardiqueWeb.Entity.Payment;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    public byte[] generatePaymentReceipt(Payment payment) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

        document.add(new Paragraph("COMPROBANTE DE PAGO", titleFont));
        document.add(new Paragraph("Sociedad Portuaria Mardique", normalFont));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        addRow(table, "ID Pago:", String.valueOf(payment.getId()), headerFont, normalFont);
        addRow(table, "Cliente:", payment.getUsername() != null ? payment.getUsername() : "-", headerFont, normalFont);
        addRow(table, "Concepto:", payment.getConcepto() != null ? payment.getConcepto() : "-", headerFont, normalFont);
        addRow(table, "Monto:", "$ " + (payment.getMonto() != null ? String.format("%,.2f", payment.getMonto()) : "0.00") + " " + (payment.getMoneda() != null ? payment.getMoneda() : "COP"), headerFont, normalFont);
        addRow(table, "Referencia:", payment.getReferencia() != null ? payment.getReferencia() : "-", headerFont, normalFont);
        addRow(table, "Fecha Pago:", payment.getFechaPago() != null ? payment.getFechaPago().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-", headerFont, normalFont);
        addRow(table, "Estado:", "PROCESADO", headerFont, normalFont);

        document.add(table);
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("Este comprobante fue generado autom\u00e1ticamente por el sistema.", smallFont));
        document.add(new Paragraph("Sociedad Portuaria Mardique S.A.S.", smallFont));

        document.close();
        return baos.toByteArray();
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell1 = new PdfPCell(new Phrase(label, labelFont));
        cell1.setBorderWidth(0f);
        cell1.setPadding(6);
        cell1.setBackgroundColor(new Color(245, 247, 250));

        PdfPCell cell2 = new PdfPCell(new Phrase(value, valueFont));
        cell2.setBorderWidth(0f);
        cell2.setPadding(6);

        table.addCell(cell1);
        table.addCell(cell2);
    }
}
