package edu.connexion3a8.services.collaboration;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import edu.connexion3a8.entities.collaboration.Sale;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;

public class ExportService {

    private static final Color PRIMARY_COLOR = new Color(79, 70, 229); // Indigo 600
    private static final Color SECONDARY_COLOR = new Color(107, 114, 128); // Gray 500
    private static final Color TEXT_DARK = new Color(17, 24, 39); // Gray 900
    private static final Color BACKGROUND_LIGHT = new Color(243, 244, 246); // Gray 100

    public void exportSaleToPdf(Sale sale, File file) throws Exception {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        try {
            // Fonts
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, PRIMARY_COLOR);
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, TEXT_DARK);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 11, TEXT_DARK);
            Font lightFont = FontFactory.getFont(FontFactory.HELVETICA, 10, SECONDARY_COLOR);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);

            // 1. Header (Company details and Invoice title)
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[] { 1f, 1f });

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.addElement(new Paragraph("INVOICE", headerFont));
            leftCell.addElement(new Paragraph("INV-" + sale.getReference(), lightFont));
            headerTable.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph companyName = new Paragraph("PIDEV Admin Inc.", subHeaderFont);
            companyName.setAlignment(Element.ALIGN_RIGHT);
            Paragraph companyAddress = new Paragraph("123 Business Avenue\nTech District, 10001\ncontact@pidev.admin",
                    lightFont);
            companyAddress.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(companyName);
            rightCell.addElement(companyAddress);
            headerTable.addCell(rightCell);

            document.add(headerTable);
            document.add(new Paragraph("\n"));
            document.add(new LineSeparator(1f, 100f, BACKGROUND_LIGHT, Element.ALIGN_CENTER, -5f));
            document.add(new Paragraph("\n"));

            // 2. Billing & Shipping Info
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            VBoxCell(infoTable, "BILLED TO:", String.valueOf(sale.getCustomerId()), sale.getBillingAddress(),
                    subHeaderFont, lightFont, regularFont);
            VBoxCell(infoTable, "SHIPPED TO:", "", sale.getShippingAddress(), subHeaderFont, lightFont, regularFont);

            document.add(infoTable);
            document.add(new Paragraph("\n\n"));

            // 3. Order Details Table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 4f, 2f, 2f, 2f });
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Table Header
            addTableHeader(table, "Item Description", tableHeaderFont);
            addTableHeader(table, "Qty", tableHeaderFont);
            addTableHeader(table, "Price", tableHeaderFont);
            addTableHeader(table, "Total", tableHeaderFont);

            // Table Row (Single item for now, based on sale.getProductId())
            addTableCell(table, "Product ID: " + sale.getProductId(), regularFont);
            addTableCell(table, "1", regularFont);
            String amountStr = String.format("%.2f %s", sale.getTotalAmount(), sale.getCurrency());
            addTableCell(table, amountStr, regularFont);
            addTableCell(table, amountStr, regularFont);

            document.add(table);

            // 4. Totals
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(100);
            totalTable.setWidths(new float[] { 3f, 1f });

            PdfPCell emptyCell = new PdfPCell(new Phrase(""));
            emptyCell.setBorder(Rectangle.NO_BORDER);
            totalTable.addCell(emptyCell);

            PdfPCell totalCell = new PdfPCell();
            totalCell.setBorder(Rectangle.TOP);
            totalCell.setBorderColor(BACKGROUND_LIGHT);
            totalCell.setPaddingTop(10f);
            Paragraph totalParagraph = new Paragraph("Grand Total: " + amountStr, subHeaderFont);
            totalParagraph.setAlignment(Element.ALIGN_RIGHT);
            totalCell.addElement(totalParagraph);
            totalTable.addCell(totalCell);

            document.add(totalTable);

            // 5. Footer (Notes & Payment Status)
            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("Payment Status: " + sale.getStatus().toUpperCase(), subHeaderFont));
            document.add(new Paragraph("Payment Method: " + sale.getPaymentMethod(), regularFont));
            if (sale.getNotes() != null && !sale.getNotes().trim().isEmpty()) {
                document.add(new Paragraph("\nNotes:\n" + sale.getNotes(), lightFont));
            }

        } finally {
            document.close();
            writer.close();
        }
    }

    private void addTableHeader(PdfPTable table, String headerTitle, Font font) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(PRIMARY_COLOR);
        header.setBorderWidth(0);
        header.setPaddingBottom(8f);
        header.setPaddingTop(8f);
        header.setPaddingLeft(5f);
        header.setPhrase(new Phrase(headerTitle, font));
        table.addCell(header);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderColor(BACKGROUND_LIGHT);
        cell.setPadding(8f);
        table.addCell(cell);
    }

    private void VBoxCell(PdfPTable table, String title, String name, String address, Font titleFont, Font lightFont,
            Font regFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.addElement(new Paragraph(title, lightFont));
        if (name != null && !name.isEmpty()) {
            cell.addElement(new Paragraph("Customer #" + name, titleFont));
        }
        if (address != null && !address.trim().isEmpty()) {
            cell.addElement(new Paragraph(address, regFont));
        } else {
            cell.addElement(new Paragraph("No address provided.", lightFont));
        }
        table.addCell(cell);
    }
}
