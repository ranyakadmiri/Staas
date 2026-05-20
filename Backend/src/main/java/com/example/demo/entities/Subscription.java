package com.example.demo.entities;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.FieldDefaults;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level= AccessLevel.PRIVATE)
@Entity
@Data
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Project project;

    @ManyToOne
    private BillingPlan plan;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;    // ACTIVE, EXPIRED, CANCELLED

    private LocalDate startDate;
    private LocalDate endDate;            // null for PAYG

    private LocalDateTime createdAt = LocalDateTime.now();
}
