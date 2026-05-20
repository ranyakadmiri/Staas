package com.example.demo.services;

import com.example.demo.entities.Invoice;

import org.springframework.stereotype.Service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;

import java.io.File;
import java.io.File;
@Service
public class PdfService {

    private static final String OUTPUT_DIR = "invoices/";

    public String generatePdf(Invoice invoice) throws Exception {

        new File(OUTPUT_DIR).mkdirs();
        String filename = OUTPUT_DIR + invoice.getInvoiceNumber() + ".pdf";

        PdfWriter writer = new PdfWriter(filename);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        doc.add(new Paragraph("INVOICE")
                .setBold().setFontSize(24));
        doc.add(new Paragraph("Invoice #: " + invoice.getInvoiceNumber()));
        doc.add(new Paragraph("Date:      " + invoice.getIssuedAt().toLocalDate()));
        doc.add(new Paragraph("Due:       " + invoice.getDueDate()));
        doc.add(new Paragraph("Project:   " + invoice.getProject().getName()));
        doc.add(new Paragraph("Period:    " + invoice.getBillingPeriod()));
        doc.add(new Paragraph(" "));

        Table table = new Table(2);
        table.addCell("Service");             table.addCell("Cost (TND)");
        table.addCell("Object Storage");      table.addCell(invoice.getObjectStorageCost().toString());
        table.addCell("Block Storage");       table.addCell(invoice.getBlockStorageCost().toString());
        table.addCell("Filesystem Storage");  table.addCell(invoice.getFilesystemCost().toString());
        table.addCell("TOTAL");               table.addCell(invoice.getTotalAmount().toString());
        doc.add(table);

        doc.close();

        return filename;    // return path instead of setting it here
    }
}