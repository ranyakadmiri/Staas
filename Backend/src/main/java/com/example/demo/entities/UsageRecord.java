package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level= AccessLevel.PRIVATE)
@Entity
public class UsageRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)        // ADD THIS — fixes 0/1/2 → OBJECT/BLOCK/FILE
    private ResourceType resourceType;   // STORAGE / REQUESTS / TRANSFER

    private Double usedGB;
    private LocalDateTime recordedAt;

    private String billingPeriod;

    @ManyToOne
    @JoinColumn(name = "bucket_id", nullable = true)
    private Bucket bucket;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;
}
