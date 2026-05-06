package com.example.demo.dto;

import lombok.Data;

@Data
public class CreateBlockVolumeRequest {
    private String name;
    private int sizeGB;
    private String initiatorIqn;
}