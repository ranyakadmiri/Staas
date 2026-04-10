package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class CredentialDTO {
    private Long id;

    private String accessKey;

    private String rgwUid;

    private boolean active;

    private String projectName;

    private Long projectId;

    private LocalDateTime createdAt;
}
