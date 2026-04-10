package com.example.demo.dto;

import com.example.demo.entities.ResourceType;
import lombok.Data;

@Data
public class CreateProjectRequest {
    private String name;

    private String description;

    private ResourceType resourceType;

    private String region;

    private Long maxBuckets;

    private Long maxStorageGB;
}
