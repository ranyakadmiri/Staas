package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MountInfoResponse {
    private String server;
    private String path;
    private String mountTarget;
    private String protocol;
    private String esxiVersion;
}