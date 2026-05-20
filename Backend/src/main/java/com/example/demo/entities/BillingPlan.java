package com.example.demo.entities;

import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.FieldDefaults;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level= AccessLevel.PRIVATE)
@Entity
@Data
public class BillingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;                  // "PAYG", "PACK_1M", "PACK_3M", "PACK_6M", "PACK_12M"

    @Enumerated(EnumType.STRING)
    private PlanType type;                // PAY_AS_YOU_GO, PACK

    private Integer durationMonths;       // null for PAYG

    // pricing per GB per month
    private BigDecimal pricePerGBObject;       // object storage
    private BigDecimal pricePerGBBlock;        // block storage
    private BigDecimal pricePerGBFilesystem;   // filesystem storage

    // pack: flat included GB
    private Long includedStorageGB;       // null for PAYG

    private BigDecimal flatPrice;         // total pack price, null for PAYG
}
