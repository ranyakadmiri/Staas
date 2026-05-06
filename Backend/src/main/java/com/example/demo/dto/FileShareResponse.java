package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileShareResponse {
    private Long id;
    private Long projectId;
    private String name;
    private String shareKey;
    private String pseudoPath;
    private String realPath;
    private String serverIp;
    private Integer exportId;
    private String status;
    private String mountTarget;
}