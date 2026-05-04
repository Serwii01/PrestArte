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

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // --- FUENTES Y COLORES CORPORATIVOS ---
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(44, 62, 80));
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Color accentColor = new Color(41, 128, 185); // Azul Prestarte

            // --- 1. CABECERA CON LOGO REAL ---
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1.5f, 3.5f});

            try {
                // Ruta que me indicaste: src/main/resources/static/images/logo.png
                Image logo = Image.getInstance("src/main/resources/static/images/logo.png");
                logo.scaleToFit(80, 80);
                PdfPCell logoCell = new PdfPCell(logo);
                logoCell.setBorder(Rectangle.NO_BORDER);
                header.addCell(logoCell);
            } catch (Exception e) {
                header.addCell(new PdfPCell(new Phrase("PRESTARTE LOGO", labelFont)));
            }

            PdfPCell titleCell = new PdfPCell(new Phrase("CONTRATO FORMAL DE PRÉSTAMO E ITINERARIO", titleFont));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            header.addCell(titleCell);
            document.add(header);

            document.add(new Paragraph("\n"));

            // --- 2. IDENTIFICACIÓN LEGAL (DNI / CIF) ---
            addSectionHeader(document, "I. IDENTIFICACIÓN DE LAS PARTES", sectionFont, accentColor);
            PdfPTable partiesTable = createTable(2);
            // Datos del Coleccionista
            addInfoRow(partiesTable, "COLECCIONISTA:", loan.getArtwork().getCollector().getName(), labelFont, valueFont);
            addInfoRow(partiesTable, "DNI/NIF:", "77889900X", labelFont, valueFont); // Asegúrate de tener este campo en tu entidad
            // Datos de la Fundación
            addInfoRow(partiesTable, "FUNDACIÓN RECEPTORA:", loan.getFoundation().getName(), labelFont, valueFont);
            addInfoRow(partiesTable, "CIF:", "G-12345678", labelFont, valueFont); // Asegúrate de tener este campo en tu entidad
            document.add(partiesTable);

            // --- 3. ESPECIFICACIONES TÉCNICAS DE LA OBRA ---
            addSectionHeader(document, "II. DATOS DEL OBJETO ARTÍSTICO", sectionFont, accentColor);
            PdfPTable artTable = createTable(2);
            addInfoRow(artTable, "TÍTULO DE LA OBRA:", loan.getArtwork().getTitle(), labelFont, valueFont);
            addInfoRow(artTable, "ARTISTA:", loan.getArtwork().getArtist(), labelFont, valueFont);
            addInfoRow(artTable, "DIMENSIONES:", loan.getArtwork().getWidthCm() + "x" + loan.getArtwork().getHeightCm() + " cm", labelFont, valueFont);
            addInfoRow(artTable, "ESTADO DE CONSERVACIÓN:", loan.getArtwork().getCondition().name(), labelFont, valueFont);
            document.add(artTable);

            // --- 4. LOGÍSTICA DE TRANSPORTE ---
            addSectionHeader(document, "III. DETALLES DEL TRASLADO Y OPERADOR", sectionFont, accentColor);
            PdfPTable transportTable = createTable(2);

            if (shipment != null) {
                // Accedemos a la empresa y luego a su nombre (asegúrate que en TransportCompany el campo sea 'companyName')
                String nombreEmpresa = (shipment.getTransportCompany() != null)
                        ? shipment.getTransportCompany().getCompanyName()
                        : "Empresa no asignada";

                addInfoRow(transportTable, "EMPRESA DE TRANSPORTE:", nombreEmpresa, labelFont, valueFont);
                addInfoRow(transportTable, "Nº SEGUIMIENTO (TRACKING):",
                        shipment.getTrackingNumber() != null ? shipment.getTrackingNumber() : "Pendiente de asignar",
                        labelFont, valueFont);
                addInfoRow(transportTable, "MÉTODO DE ENVÍO:", "Transporte Especializado de Arte", labelFont, valueFont);
            } else {
                addInfoRow(transportTable, "ESTADO:", "Pendiente de asignar transporte", labelFont, valueFont);
            }
            document.add(transportTable);

            // --- 5. CLÁUSULAS Y CONDICIONES ESPECIALES ---
            addSectionHeader(document, "IV. CLÁUSULAS ESPECÍFICAS DE PRÉSTAMO", sectionFont, accentColor);
            String conditionsText = (loan.getArtwork().getLoanConditions() != null) ?
                    loan.getArtwork().getLoanConditions() : "No se han estipulado condiciones adicionales.";
            Paragraph cond = new Paragraph(conditionsText, valueFont);
            cond.setSpacingBefore(8);
            cond.setSpacingAfter(15);
            cond.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(cond);

            // --- 6. VIGENCIA Y FIRMAS ---
            document.add(new Paragraph("VIGENCIA: Este contrato es válido desde " + loan.getStartDate() + " hasta " + loan.getEndDate(), labelFont));

            document.add(new Paragraph("\n\n"));
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.addCell(createSignatureCell("Firma Coleccionista", labelFont));
            footerTable.addCell(createSignatureCell("Firma Responsable Fundación", labelFont));
            document.add(footerTable);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error detallado en PDF: " + e.getMessage());
        }

        return out.toByteArray();
    }

    // Métodos auxiliares de diseño
    private void addSectionHeader(Document doc, String text, Font font, Color color) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(color);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(6);
        table.addCell(cell);
        doc.add(table);
    }

    private PdfPTable createTable(int columns) {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(5);
        return table;
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font lFont, Font vFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, lFont));
        c1.setBorder(Rectangle.BOTTOM);
        c1.setBorderColor(Color.LIGHT_GRAY);
        c1.setPadding(5);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, vFont));
        c2.setBorder(Rectangle.BOTTOM);
        c2.setBorderColor(Color.LIGHT_GRAY);
        c2.setPadding(5);
        table.addCell(c2);
    }

    private PdfPCell createSignatureCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase("\n\n__________________________\n" + text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }
}