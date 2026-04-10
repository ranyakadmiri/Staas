package com.example.demo.dto;

import lombok.Data;

@Data
public class IscsiVolumeDTO {
    private String name;               // ex: esxi-disk-02
    private int sizeGB;               // ex: 10
    private String initiatorIqn;       // ESXi IQN
}