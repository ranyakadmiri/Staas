package com.example.demo.dto;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class BucketStatsDTO {

    private String bucketName;
    private int objectCount;
    private long totalSizeBytes;
    private double totalSizeMB;
    private double totalSizeGB;

    private String largestObject;
    private long largestObjectSize;

    private String lastObject;
    private LocalDateTime lastModified;

    private Instant creationDate;

}