package com.example.demo.dto;

import com.example.demo.entities.BlockVolumeStatus;
import com.example.demo.entities.InitiatorType;
import lombok.Builder;
import lombok.Data;

/**
 * API response for a block volume.
 *
 * <p>The {@code iscsiConnection} block is always populated and gives the
 * client everything needed to connect with any iSCSI initiator.
 *
 * <p>The {@code esxiIntegration} block is non-null only when the volume was
 * created with ESXi integration enabled.
 */
@Data
@Builder
public class BlockVolumeResponse {

    private Long id;
    private Long projectId;
    private String name;
    private String volumeKey;
    private int sizeGB;
    private InitiatorType initiatorType;

    @Builder.Default
    private BlockVolumeStatus status = BlockVolumeStatus.PENDING;

    private String errorMessage;

    /** Always present — use these credentials with any iSCSI client. */
    private IscsiConnectionInfo iscsiConnection;

    /**
     * Present only when ESXi integration was requested.
     * Null for generic iSCSI volumes.
     */
    private EsxiIntegrationInfo esxiIntegration;

    // ── Nested response objects ───────────────────────────────────────────────

    @Data
    @Builder
    public static class IscsiConnectionInfo {
        private String targetIqn;
        private String portalAddress;   // e.g. "192.168.100.51:3260"
        private String initiatorIqn;
    }

    @Data
    @Builder
    public static class EsxiIntegrationInfo {
        private String datastoreName;
        private String esxiDiskCanonical;
    }
}