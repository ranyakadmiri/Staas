package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "block_volumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockVolume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long projectId;

    private String name;          // user-visible name
    private String volumeKey;     // e.g. proj-9-db-disk

    private int sizeGB;

    private String poolName;

    // ── iSCSI fields (always present) ────────────────────────────────────────
    private String targetIqn;
    private String initiatorIqn;
    private String gatewayIp;

    /**
     * Distinguishes generic iSCSI clients from VMware ESXi.
     * Determines which provisioning path is executed and which terminal
     * status ({@link BlockVolumeStatus#ISCSI_READY} vs {@link BlockVolumeStatus#READY})
     * is used.
     */
    @Enumerated(EnumType.STRING)
    private InitiatorType initiatorType;

    // ── ESXi-specific fields (nullable for generic iSCSI volumes) ────────────
    /** Name of the VMFS datastore created on ESXi. Null for generic volumes. */
    private String datastoreName;

    /** Canonical disk name detected on ESXi after LUN presentation. Null for generic volumes. */
    private String esxiDiskCanonical;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    private BlockVolumeStatus status;

    @Column(length = 2000)
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}