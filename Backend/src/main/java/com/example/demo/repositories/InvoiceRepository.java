package com.example.demo.repositories;


import com.example.demo.entities.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    long countByBillingPeriod(String period);
    List<Invoice> findByProjectIdOrderByIssuedAtDesc(Long projectId);
}
