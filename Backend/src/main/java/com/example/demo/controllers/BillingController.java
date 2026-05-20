package com.example.demo.controllers;
import com.example.demo.entities.*;
import com.example.demo.repositories.BillingPlanRepository;
import com.example.demo.repositories.InvoiceRepository;
import com.example.demo.repositories.SubscriptionRepository;
import com.example.demo.repositories.UsageRecordRepository;
import com.example.demo.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpHeaders;

import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;

// Client endpoints
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final PdfService pdfService;
    private final UsageCollectorService usageCollectorService;
    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BillingPlanRepository billingPlanRepository;
    private final UsageRecordRepository usageRecordRepository;
    @GetMapping("/invoices/{projectId}")
    public ResponseEntity<?> getInvoices(@PathVariable Long projectId) {
        return ResponseEntity.ok(
                invoiceRepository.findByProjectIdOrderByIssuedAtDesc(projectId)
        );
    }

    @GetMapping("/invoices/{invoiceId}/pdf")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long invoiceId) throws Exception {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // regenerate if missing
        if (invoice.getPdfPath() == null || !new File(invoice.getPdfPath()).exists()) {
            String pdfPath = pdfService.generatePdf(invoice);
            invoice.setPdfPath(pdfPath);
            invoiceRepository.save(invoice);
        }

        Path path = Paths.get(invoice.getPdfPath());
        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + invoice.getInvoiceNumber() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @PostMapping("/subscribe/{projectId}")
    public ResponseEntity<?> subscribe(
            @PathVariable Long projectId,
            @RequestParam Long planId) {
        return ResponseEntity.ok(billingService.subscribe(projectId, planId));
    }
    // In BillingController.java — remove after testing
    @PostMapping("/test/collect-usage")
    public ResponseEntity<?> triggerUsageCollection() {
        usageCollectorService.collectDailyUsage();
        return ResponseEntity.ok("Usage collected");
    }
    // In BillingController.java — remove after testing
    @PostMapping("/test/generate-invoice/{projectId}")
    public ResponseEntity<?> triggerInvoice(@PathVariable Long projectId) {

        Subscription sub = subscriptionRepository
                .findByProjectIdAndStatus(projectId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription"));

        String period = YearMonth.now().toString();   // "2026-05"

        Invoice invoice = billingService.generateInvoice(sub, period);
        return ResponseEntity.ok(invoice);
    }
    // In BillingController.java
    @GetMapping("/plans")
    public ResponseEntity<?> getPlans() {
        return ResponseEntity.ok(billingPlanRepository.findAll());
    }
    // Add to BillingController.java

    @GetMapping("/usage/{projectId}")
    public ResponseEntity<?> getUsage(@PathVariable Long projectId) {
        return ResponseEntity.ok(
                usageRecordRepository
                        .findByProjectIdAndBillingPeriodAndBucketIsNull(
                                projectId,
                                YearMonth.now().toString()
                        )
        );
    }

    @GetMapping("/subscription/{projectId}")
    public ResponseEntity<?> getSubscription(@PathVariable Long projectId) {
        return ResponseEntity.ok(
                subscriptionRepository
                        .findByProjectIdAndStatus(projectId, SubscriptionStatus.ACTIVE)
                        .orElseThrow(() -> new RuntimeException("No active subscription"))
        );
    }
}

