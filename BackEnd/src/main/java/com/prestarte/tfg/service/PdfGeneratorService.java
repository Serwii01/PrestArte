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

/**
 * Servicio que genera el contrato del préstamo en formato PDF.
 *
 * Compone un documento con cabecera, datos de las partes (coleccionista,
 * fundación y empresa de transporte), información de la obra y del
 * envío, condiciones acordadas y pie de firma, usando la biblioteca
 * OpenPDF. El PDF se utiliza tanto en las notificaciones por correo
 * como en la descarga manual desde la ficha del préstamo.
 */
@Service
public class PdfGeneratorService {

    /**
     * Genera los bytes del contrato del préstamo indicado, incluyendo
     * los datos económicos del envío asociado.
     */
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
                // Desde el classpath: la ruta "src/main/resources/..." solo existe
                // al ejecutar desde el IDE, no dentro del jar ni del contenedor.
                byte[] logoBytes;
                try (var in = new org.springframework.core.io.ClassPathResource("static/images/logo.png").getInputStream()) {
                    logoBytes = in.readAllBytes();
                }
                Image logo = Image.getInstance(logoBytes);
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
            Double insured = loan.getAgreedValue() != null ? loan.getAgreedValue() : loan.getArtwork().getEstimatedValue();
            addInfoRow(artTable, "VALOR ASEGURADO ACORDADO:", String.format("%.2f €", insured), labelFont, valueFont);
            if (loan.getExhibition() != null) {
                addInfoRow(artTable, "EXPOSICIÓN:", loan.getExhibition().getTitle() + " — "
                        + loan.getExhibition().getVenueName() + ", " + loan.getExhibition().getVenueCity(), labelFont, valueFont);
                // Exposición itinerante: el préstamo cubre todas las sedes de la gira.
                for (var stop : loan.getExhibition().getStops()) {
                    addInfoRow(artTable, "SEDE " + (stop.getPosition() + 1) + ":", stop.getHostInstitution() + " — "
                            + stop.getVenueName() + ", " + stop.getVenueCity() + " (" + stop.getOpeningDate().format(formatter)
                            + " – " + stop.getClosingDate().format(formatter) + ")", labelFont, valueFont);
                }
            }
            addInfoRow(artTable, "PERIODO DEL PRÉSTAMO:", loan.getStartDate().format(formatter) + " – "
                    + loan.getEndDate().format(formatter), labelFont, valueFont);
            document.add(artTable);

            // --- Condiciones del prestador: cartela, imagen y canon ---
            addSectionHeader(document, "CONDICIONES DEL PRESTADOR", sectionFont, accentColor);
            PdfPTable termsTable = createTable(2);
            String credit = loan.getArtwork().getCreditLine() != null ? loan.getArtwork().getCreditLine() : "Colección particular";
            addInfoRow(termsTable, "MENCIÓN EN CARTELA:", credit, labelFont, valueFont);
            addInfoRow(termsTable, "USOS DE IMAGEN AUTORIZADOS:", describeImageRights(loan.getArtwork().getImageRights()), labelFont, valueFont);
            Double fee = loan.getArtwork().getLoanFee();
            addInfoRow(termsTable, "CANON DE PRÉSTAMO:", fee != null && fee > 0 ? String.format("%.2f €", fee) : "Gratuito", labelFont, valueFont);
            if (loan.getAgreedConditions() != null && !loan.getAgreedConditions().isBlank()) {
                addInfoRow(termsTable, "CONDICIONES ACORDADAS:", loan.getAgreedConditions(), labelFont, valueFont);
            }
            document.add(termsTable);

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

    /**
     * Certificado de exposición: acredita que la obra se expuso en una
     * institución. Forma parte del historial de la obra y aumenta su valor
     * de procedencia, uno de los motivos por los que un coleccionista presta.
     */
    public byte[] generateExhibitionCertificate(LoanRequest loan) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 60, 60, 60, 60);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", java.util.Locale.forLanguageTag("es-ES"));
        Color accent = new Color(41, 128, 185);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font small = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, new Color(44, 62, 80));
            Font big = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, accent);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 13, Color.DARK_GRAY);
            Font bodyBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.DARK_GRAY);

            Paragraph brand = new Paragraph("PRESTARTE", small);
            brand.setAlignment(Element.ALIGN_CENTER);
            document.add(brand);

            Paragraph t = new Paragraph("CERTIFICADO DE EXPOSICIÓN", title);
            t.setAlignment(Element.ALIGN_CENTER);
            t.setSpacingBefore(10);
            t.setSpacingAfter(30);
            document.add(t);

            Paragraph intro = new Paragraph("Se certifica que la obra", body);
            intro.setAlignment(Element.ALIGN_CENTER);
            document.add(intro);

            Paragraph art = new Paragraph("«" + loan.getArtwork().getTitle() + "»", big);
            art.setAlignment(Element.ALIGN_CENTER);
            art.setSpacingBefore(6);
            document.add(art);

            String artist = loan.getArtwork().getArtist()
                    + (loan.getArtwork().getYear() != null ? ", " + loan.getArtwork().getYear() : "");
            Paragraph a = new Paragraph(artist, bodyBold);
            a.setAlignment(Element.ALIGN_CENTER);
            a.setSpacingAfter(20);
            document.add(a);

            var ex = loan.getExhibition();
            Paragraph p = new Paragraph();
            p.setAlignment(Element.ALIGN_CENTER);
            p.add(new Chunk("ha formado parte de la exposición ", body));
            p.add(new Chunk("«" + ex.getTitle() + "»", bodyBold));
            p.add(new Chunk(", organizada por ", body));
            p.add(new Chunk(loan.getFoundation().getName(), bodyBold));
            p.add(new Chunk(" en " + ex.getVenueName() + " (" + ex.getVenueCity() + "), del "
                    + ex.getOpeningDate().format(formatter) + " al " + ex.getClosingDate().format(formatter)
                    + (ex.isItinerant() ? "," : "."), body));
            if (ex.isItinerant()) {
                // Solo las sedes a las que la obra llegó de verdad.
                int reached = loan.getCurrentStop() != null ? loan.getCurrentStop() : 0;
                var visited = ex.getStops().stream().filter(s -> s.getPosition() <= reached).toList();
                if (visited.isEmpty()) {
                    p.add(new Chunk(" primera sede de una exposición itinerante.", body));
                } else {
                    p.add(new Chunk(" y viajó con la exposición a ", body));
                    for (int i = 0; i < visited.size(); i++) {
                        var s = visited.get(i);
                        p.add(new Chunk(s.getHostInstitution() + " (" + s.getVenueCity() + ")", bodyBold));
                        p.add(new Chunk(i == visited.size() - 1 ? "." : i == visited.size() - 2 ? " y " : ", ", body));
                    }
                }
            }
            document.add(p);

            String credit = loan.getArtwork().getCreditLine() != null ? loan.getArtwork().getCreditLine() : "Colección particular";
            Paragraph c = new Paragraph("Prestada por: " + credit, body);
            c.setAlignment(Element.ALIGN_CENTER);
            c.setSpacingBefore(20);
            document.add(c);

            Paragraph ref = new Paragraph("Referencia del préstamo: PA-" + loan.getId()
                    + " · Emitido el " + java.time.LocalDate.now().format(formatter), small);
            ref.setAlignment(Element.ALIGN_CENTER);
            ref.setSpacingBefore(40);
            document.add(ref);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el certificado de exposición: " + e.getMessage());
        }
        return out.toByteArray();
    }

    // --- MÉTODOS AUXILIARES ---

    private static String describeImageRights(com.prestarte.tfg.model.entity.ImageRights r) {
        if (r == null) return "Ninguno (solicitar autorización expresa)";
        java.util.List<String> uses = new java.util.ArrayList<>();
        if (Boolean.TRUE.equals(r.getCatalog())) uses.add("catálogo");
        if (Boolean.TRUE.equals(r.getPress())) uses.add("prensa");
        if (Boolean.TRUE.equals(r.getSocialMedia())) uses.add("web y redes sociales");
        if (Boolean.TRUE.equals(r.getMerchandise())) uses.add("productos de tienda");
        if (Boolean.TRUE.equals(r.getVisitorPhotos())) uses.add("fotografía del público sin flash");
        return uses.isEmpty() ? "Ninguno (solicitar autorización expresa)" : String.join(", ", uses);
    }
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