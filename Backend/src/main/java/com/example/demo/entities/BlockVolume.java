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

    private String name;           // user-visible name
    private String volumeKey;      // proj-9-db-disk
    private int sizeGB;

    private String poolName;
    private String targetIqn;
    private String initiatorIqn;
    private String gatewayIp;

    private String datastoreName;
    private String esxiDiskCanonical;

    @Enumerated(EnumType.STRING)
    private BlockVolumeStatus status;

    @Column(length = 2000)
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}