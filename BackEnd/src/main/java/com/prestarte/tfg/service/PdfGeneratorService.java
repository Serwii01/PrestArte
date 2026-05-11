package com.prestarte.tfg.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.model.entity.Shipment;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGeneratorService {

    public byte[] generateLoanContract(LoanRequest loan, Shipment shipment) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // --- FUENTES Y COLORES ---
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(44, 62, 80));
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Color accentColor = new Color(41, 128, 185);

            // --- 1. CABECERA ---
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1.2f, 3.8f});

            try {
                Image logo = Image.getInstance("src/main/resources/static/images/logo.png");
                logo.scaleToFit(65, 65);
                PdfPCell logoCell = new PdfPCell(logo);
                logoCell.setBorder(Rectangle.NO_BORDER);
                header.addCell(logoCell);
            } catch (Exception e) {
                header.addCell(new PdfPCell(new Phrase("PRESTARTE", labelFont)));
            }

            PdfPCell titleCell = new PdfPCell(new Phrase("CERTIFICADO DE PRÉSTAMO Y COBERTURA DE TRANSPORTE", titleFont));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            header.addCell(titleCell);
            document.add(header);
            document.add(new Paragraph("\n"));

            // --- 2. PARTES ---
            addSectionHeader(document, "I. IDENTIFICACIÓN DE LAS PARTES", sectionFont, accentColor);
            PdfPTable partiesTable = createTable(2);
            addInfoRow(partiesTable, "PARTE PRESTAMISTA (COLECCIONISTA):", loan.getArtwork().getCollector().getName(), labelFont, valueFont);
            addInfoRow(partiesTable, "PARTE RECEPTORA (FUNDACIÓN):", loan.getFoundation().getName(), labelFont, valueFont);
            document.add(partiesTable);

            // --- 3. LA OBRA Y SU VALOR ---
            addSectionHeader(document, "II. ESPECIFICACIONES DE LA OBRA ASEGURADA", sectionFont, accentColor);
            PdfPTable artTable = createTable(2);
            addInfoRow(artTable, "TÍTULO:", loan.getArtwork().getTitle(), labelFont, valueFont);
            addInfoRow(artTable, "VALOR ESTIMADO (ASEGURADO):", String.format("%.2f €", loan.getArtwork().getEstimatedValue()), labelFont, valueFont);
            document.add(artTable);

            // --- 4. LOGÍSTICA Y SEGURO (Novedad) ---
            addSectionHeader(document, "III. LOGÍSTICA Y COBERTURA DE SEGURO NAIL-TO-NAIL", sectionFont, accentColor);
            PdfPTable transportTable = createTable(2);

            if (shipment != null) {
                addInfoRow(transportTable, "OPERADOR LOGÍSTICO:", shipment.getTransportCompany().getCompanyName(), labelFont, valueFont);
                addInfoRow(transportTable, "Nº SEGUIMIENTO:", shipment.getTrackingNumber(), labelFont, valueFont);
                addInfoRow(transportTable, "PÓLIZA DE SEGURO Nº:",
                        shipment.getInsurancePolicy() != null ? shipment.getInsurancePolicy() : "PENDIENTE", labelFont, valueFont);
                addInfoRow(transportTable, "PRIMA DE SEGURO:",
                        (shipment.getInsuranceCost() != null) ? String.format("%.2f €", shipment.getInsuranceCost()) : "Pendiente de tasar",
                        labelFont, valueFont);
            }
            document.add(transportTable);

            // --- 5. CLÁUSULA LEGAL ---
            addSectionHeader(document, "IV. CLÁUSULAS DE RESPONSABILIDAD", sectionFont, accentColor);
            String legalClause = "La cobertura de seguro 'Clavo a Clavo' garantiza la integridad de la pieza desde su descolgado en origen hasta su instalación en destino. " +
                    "Cualquier incidencia detectada durante el transporte deberá ser notificada de inmediato a través de la plataforma PrestArte.";
            Paragraph pLegal = new Paragraph(legalClause, FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
            pLegal.setSpacingBefore(5);
            pLegal.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(pLegal);

            // --- 6. FIRMAS ---
            document.add(new Paragraph("\n\n"));
            PdfPTable footerTable = new PdfPTable(3);
            footerTable.setWidthPercentage(100);
            footerTable.addCell(createSignatureCell("Firma Prestador", labelFont));
            footerTable.addCell(createSignatureCell("Firma Fundación", labelFont));
            footerTable.addCell(createSignatureCell("Sello Transportista", labelFont));
            document.add(footerTable);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el contrato PDF: " + e.getMessage());
        }

        return out.toByteArray();
    }

    // --- MÉTODOS AUXILIARES ---
    private void addSectionHeader(Document doc, String text, Font font, Color color) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(color);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5);
        table.addCell(cell);
        doc.add(table);
    }

    private PdfPTable createTable(int columns) {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        return table;
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font lFont, Font vFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, lFont));
        c1.setBorder(Rectangle.BOTTOM);
        c1.setBorderColor(Color.LIGHT_GRAY);
        c1.setPadding(4);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, vFont));
        c2.setBorder(Rectangle.BOTTOM);
        c2.setBorderColor(Color.LIGHT_GRAY);
        c2.setPadding(4);
        table.addCell(c2);
    }

    private PdfPCell createSignatureCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase("\n\n__________________________\n" + text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }
}