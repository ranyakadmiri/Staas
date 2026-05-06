package com.example.demo.dto;

import com.example.demo.entities.BlockVolumeStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BlockVolumeResponse {
    private Long id;
    private Long projectId;
    private String name;
    private String volumeKey;
    private int sizeGB;
    private String targetIqn;
    private String initiatorIqn;
    private String gatewayIp;
    private String datastoreName;
    private String esxiDiskCanonical;
    private BlockVolumeStatus status;
    private String errorMessage;
}