package com.example.demo.entities;


import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.FieldDefaults;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level= AccessLevel.PRIVATE)
@Entity
@Data
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNumber;         // INV-2026-05-0001

    @ManyToOne
    private Project project;

    @ManyToOne
    private Subscription subscription;

    private BigDecimal totalAmount;
    private BigDecimal objectStorageCost;
    private BigDecimal blockStorageCost;
    private BigDecimal filesystemCost;

    private String billingPeriod;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;         // PAID, UNPAID, OVERDUE

    private LocalDateTime issuedAt;
    private LocalDateTime paidAt;
    private LocalDate dueDate;

    private String pdfPath;               // path to generated PDF
}
