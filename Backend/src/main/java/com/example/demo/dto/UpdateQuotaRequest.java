package com.example.demo.dto;

import lombok.Data;

@Data
public class UpdateQuotaRequest {
    private Long maxSizeGB;

    private Long maxObjects;
}
