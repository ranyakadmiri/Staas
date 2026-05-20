package com.example.demo.services;
import com.example.demo.dto.BucketStatsDTO;
import com.example.demo.entities.*;
import com.example.demo.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j

public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final BillingPlanRepository billingPlanRepository;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final ProjectRepository projectRepository;

    // runs on the 1st of every month at 8am
    @Scheduled(cron = "0 0 8 1 * *")
    public void generateMonthlyInvoices() {

        String lastMonth = YearMonth.now().minusMonths(1).toString();   // "2026-04"
        List<Subscription> active = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);

        for (Subscription sub : active) {
            try {
                Invoice invoice = generateInvoice(sub, lastMonth);
                pdfService.generatePdf(invoice);
                emailService.sendInvoice(invoice);
            } catch (Exception e) {
                log.error("Failed to generate invoice for subscription {}", sub.getId(), e);
            }
        }
    }

    public Invoice generateInvoice(Subscription sub, String period) {

        Project project = sub.getProject();
        BillingPlan plan = sub.getPlan();

        List<UsageRecord> records = usageRecordRepository
                .findByProjectIdAndBillingPeriodAndBucketIsNull(project.getId(), period);

        double objectGB = sumByType(records, ResourceType.OBJECT);
        double blockGB  = sumByType(records, ResourceType.BLOCK);
        double fsGB     = sumByType(records, ResourceType.FILE);


        BigDecimal objectCost = BigDecimal.ZERO;
        BigDecimal blockCost  = BigDecimal.ZERO;
        BigDecimal fsCost     = BigDecimal.ZERO;

        if (plan.getType() == PlanType.PAY_AS_YOU_GO) {
            objectCost = plan.getPricePerGBObject().multiply(BigDecimal.valueOf(objectGB));
            blockCost  = plan.getPricePerGBBlock().multiply(BigDecimal.valueOf(blockGB));
            fsCost     = plan.getPricePerGBFilesystem().multiply(BigDecimal.valueOf(fsGB));
        } else {
            // PACK: flat price, overage charged at PAYG rate if exceeded
            objectCost = plan.getFlatPrice();
            double totalUsed = objectGB + blockGB + fsGB;
            if (totalUsed > plan.getIncludedStorageGB()) {
                double overage = totalUsed - plan.getIncludedStorageGB();
                objectCost = objectCost.add(
                        plan.getPricePerGBObject().multiply(BigDecimal.valueOf(overage))
                );
            }
        }

        BigDecimal total = objectCost.add(blockCost).add(fsCost);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber(period));
        invoice.setProject(project);
        invoice.setSubscription(sub);
        invoice.setBillingPeriod(period);
        invoice.setObjectStorageCost(objectCost);
        invoice.setBlockStorageCost(blockCost);
        invoice.setFilesystemCost(fsCost);
        invoice.setTotalAmount(total);
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setDueDate(LocalDate.now().plusDays(15));

        Invoice saved = invoiceRepository.save(invoice);

        // generate PDF immediately and persist the path
        try {
            String pdfPath = pdfService.generatePdf(saved);
            saved.setPdfPath(pdfPath);
            saved = invoiceRepository.save(saved);   // save again with pdfPath
        } catch (Exception e) {
            log.error("PDF generation failed for invoice {}", saved.getInvoiceNumber(), e);
            // invoice is still saved, PDF can be regenerated later
        }

        return saved;
    }

    private double sumByType(List<UsageRecord> records, ResourceType type) {
        return records.stream()
                .filter(r -> r.getResourceType() == type)
                .mapToDouble(UsageRecord::getUsedGB)
                .average()    // average of daily snapshots across the month
                .orElse(0.0);
    }
    private String generateInvoiceNumber(String period) {
        // parse "2026-05" back to YearMonth just for formatting
        YearMonth ym = YearMonth.parse(period);
        long count = invoiceRepository.countByBillingPeriod(period);
        return String.format("INV-%d-%02d-%04d",
                ym.getYear(), ym.getMonthValue(), count + 1);
    }
    public Subscription subscribe(Long projectId, Long planId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        BillingPlan plan = billingPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        // cancel any existing active subscription
        subscriptionRepository.findByProjectIdAndStatus(projectId, SubscriptionStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(SubscriptionStatus.CANCELLED);
                    subscriptionRepository.save(existing);
                });

        Subscription sub = new Subscription();
        sub.setProject(project);
        sub.setPlan(plan);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartDate(LocalDate.now());

        // set end date for packs, null for PAYG
        if (plan.getType() == PlanType.PACK && plan.getDurationMonths() != null) {
            sub.setEndDate(LocalDate.now().plusMonths(plan.getDurationMonths()));
        }

        return subscriptionRepository.save(sub);
    }
}