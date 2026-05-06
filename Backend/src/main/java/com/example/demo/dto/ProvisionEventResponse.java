package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProvisionEventResponse {
    private String stepName;
    private String message;
    private boolean success;
    private LocalDateTime createdAt;
}