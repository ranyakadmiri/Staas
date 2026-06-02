package com.example.demo.services;

import com.example.demo.dto.BlockVolumeResponse;
import com.example.demo.dto.CreateBlockVolumeRequest;
import com.example.demo.entities.BlockVolume;
import com.example.demo.entities.BlockVolumeStatus;
import com.example.demo.entities.InitiatorType;
import com.example.demo.repositories.BlockVolumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Orchestrates the block volume lifecycle.
 *
 * <h3>Two provisioning paths</h3>
 * <ol>
 *   <li><b>Generic iSCSI</b> (default) — provisions an RBD image, creates a
 *       ceph-iscsi target, and returns IQN + portal address. The client
 *       connects with any standard iSCSI initiator (Linux open-iscsi, Windows
 *       iSCSI Initiator, VMware software adapter, cloud VMs, etc.).</li>
 *   <li><b>ESXi integration</b> (opt-in) — same as above, then additionally
 *       rescans an ESXi host and creates a VMFS datastore automatically.
 *       Enabled by populating {@code CreateBlockVolumeRequest.esxiIntegration}.</li>
 * </ol>
 *
 * <p>The ESXi path reuses the existing {@link EsxiIntegrationService}
 * (which delegates to the unchanged {@link EsxiService} and
 * {@link EsxiSshService}) — no ESXi logic was removed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockVolumeService {

    private static final String IQN_PREFIX  = "iqn.2026-04.com.staas:";
    private static final String GATEWAY_IP  = "192.168.100.51";
    private static final int    GATEWAY_PORT = 3260;

    private final BlockVolumeRepository    repository;
    private final ProvisionEventService    eventService;
    private final BlockStorageService      blockStorageService;
    private final IscsiProvisioningService iscsiProvisioning;
    private final EsxiIntegrationService   esxiIntegration;

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Creates and provisions a block volume.
     *
     * <p>If {@code dto.esxiIntegration} is null the call is synchronous and
     * returns as soon as the LUN is active (status = {@code ISCSI_READY}).
     * If ESXi integration is requested the call additionally rescans the ESXi
     * host and creates a VMFS datastore (status = {@code READY}).
     */
    public synchronized BlockVolumeResponse createVolume(
            Long projectId, CreateBlockVolumeRequest dto) {

        validateName(dto.getName());

        if (repository.existsByProjectIdAndName(projectId, dto.getName())) {
            throw new IllegalArgumentException("Block volume already exists in this project");
        }

        boolean esxiRequested = dto.getEsxiIntegration() != null;
        InitiatorType initiatorType = esxiRequested
                ? InitiatorType.VMWARE_ESXI
                : InitiatorType.GENERIC_ISCSI;

        String volumeKey    = buildVolumeKey(projectId, dto.getName());
        String targetIqn    = IQN_PREFIX + volumeKey;
        String datastoreName = esxiRequested
                ? resolveDatastoreName(dto.getEsxiIntegration(), volumeKey)
                : null;

        BlockVolume volume = BlockVolume.builder()
                .projectId(projectId)
                .name(dto.getName())
                .volumeKey(volumeKey)
                .sizeGB(dto.getSizeGB())
                .poolName(blockStorageService.getPool())
                .targetIqn(targetIqn)
                .initiatorIqn(dto.getInitiatorIqn())
                .gatewayIp(GATEWAY_IP)
                .initiatorType(initiatorType)
                .datastoreName(datastoreName)
                .status(BlockVolumeStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        repository.save(volume);
        eventService.addEvent(volume, "START", "Starting block volume provisioning ["
                + initiatorType + "]", true);

        try {
            // ── Path A: ESXi — snapshot disks BEFORE exposing the LUN ────────
            Set<String> disksBefore = esxiRequested
                    ? esxiIntegration.snapshotDisks()
                    : null;

            if (esxiRequested) {
                eventService.addEvent(volume, "ESXI_SNAPSHOT",
                        "Captured ESXi disks before provisioning", true);
            }

            // ── Core iSCSI provisioning (both paths) ─────────────────────────
            iscsiProvisioning.provision(volume);

            // ── Path A continuation: ESXi datastore ──────────────────────────
            if (esxiRequested) {
                String newDisk = esxiIntegration.detectAndRegisterNewDisk(volume, disksBefore);

                volume.setEsxiDiskCanonical(newDisk);
                repository.save(volume);

                esxiIntegration.createDatastore(volume, newDisk);

                volume.setStatus(BlockVolumeStatus.READY);

            } else {
                // ── Path B: generic iSCSI — done after LUN is ready ──────────
                volume.setStatus(BlockVolumeStatus.ISCSI_READY);
            }

            volume.setUpdatedAt(LocalDateTime.now());
            repository.save(volume);

            eventService.addEvent(volume, "READY",
                    esxiRequested
                            ? "Provisioning finished — VMFS datastore ready"
                            : "Provisioning finished — iSCSI LUN ready for connection",
                    true);

            return toResponse(volume);

        } catch (Exception e) {
            eventService.fail(volume, "ERROR", e.getMessage());
            throw new RuntimeException("Failed to provision block volume: " + e.getMessage(), e);
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Deletes a block volume and all associated resources.
     *
     * <p>For ESXi volumes the datastore is removed first (non-fatal on failure).
     * iSCSI and RBD teardown follows regardless of initiator type.
     */
    public synchronized void deleteVolume(Long projectId, String name) {
        BlockVolume volume = repository.findByProjectIdAndName(projectId, name)
                .orElseThrow(() -> new IllegalArgumentException("Block volume not found"));

        volume.setStatus(BlockVolumeStatus.DELETING);
        volume.setUpdatedAt(LocalDateTime.now());
        repository.save(volume);
        eventService.addEvent(volume, "DELETING", "Deleting block volume", true);

        try {
            // ESXi datastore cleanup (no-op for generic iSCSI volumes)
            if (volume.getInitiatorType() == InitiatorType.VMWARE_ESXI
                    && volume.getDatastoreName() != null) {
                esxiIntegration.deleteDatastore(volume);
            }

            // iSCSI + RBD teardown (both paths)
            iscsiProvisioning.deprovision(volume);

            volume.setStatus(BlockVolumeStatus.DELETED);
            volume.setUpdatedAt(LocalDateTime.now());
            repository.save(volume);
            eventService.addEvent(volume, "DELETED", "Block volume deleted", true);

        } catch (Exception e) {
            eventService.fail(volume, "DELETE_ERROR", e.getMessage());
            throw new RuntimeException("Failed to delete block volume: " + e.getMessage(), e);
        }
    }

    // ── Extend (append disk as VMFS extent) ───────────────────────────────────

    /**
     * Creates a new RBD image + iSCSI LUN and appends it as a VMFS extent to
     * the datastore of {@code existingVolumeName}.
     *
     * <p>This operation is only valid for volumes of type
     * {@link InitiatorType#VMWARE_ESXI}.
     */
    public synchronized BlockVolumeResponse appendDiskToVolume(
            Long projectId, String existingVolumeName, CreateBlockVolumeRequest dto) {

        BlockVolume existing = repository.findByProjectIdAndName(projectId, existingVolumeName)
                .orElseThrow(() -> new IllegalArgumentException("Base volume not found"));

        if (existing.getInitiatorType() != InitiatorType.VMWARE_ESXI) {
            throw new IllegalArgumentException(
                    "appendDiskToVolume is only supported for VMWARE_ESXI volumes");
        }

        validateName(dto.getName());

        String newVolumeKey = buildVolumeKey(projectId, dto.getName());
        String newTargetIqn = IQN_PREFIX + newVolumeKey;

        // Snapshot before exposing LUN
        Set<String> disksBefore = esxiIntegration.snapshotDisks();

        // Provision the new LUN (reuse existing initiator IQN)
        iscsiProvisioning.provisionAdditionalDisk(
                existing, newVolumeKey, newTargetIqn, dto.getSizeGB());

        sleep(5000);

        // Append as VMFS extent (not a new datastore)
        esxiIntegration.appendExtentToDatastore(existing, disksBefore);

        return toResponse(existing);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<BlockVolumeResponse> listProjectVolumes(Long projectId) {
        return repository.findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BlockVolumeResponse getVolume(Long projectId, String name) {
        return repository.findByProjectIdAndName(projectId, name)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Block volume not found"));
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private BlockVolumeResponse toResponse(BlockVolume v) {

        BlockVolumeResponse.IscsiConnectionInfo iscsiInfo =
                BlockVolumeResponse.IscsiConnectionInfo.builder()
                        .targetIqn(v.getTargetIqn())
                        .portalAddress(v.getGatewayIp() + ":" + GATEWAY_PORT)
                        .initiatorIqn(v.getInitiatorIqn())
                        .build();

        BlockVolumeResponse.EsxiIntegrationInfo esxiInfo = null;
        if (v.getInitiatorType() == InitiatorType.VMWARE_ESXI) {
            esxiInfo = BlockVolumeResponse.EsxiIntegrationInfo.builder()
                    .datastoreName(v.getDatastoreName())
                    .esxiDiskCanonical(v.getEsxiDiskCanonical())
                    .build();
        }

        return BlockVolumeResponse.builder()
                .id(v.getId())
                .projectId(v.getProjectId())
                .name(v.getName())
                .volumeKey(v.getVolumeKey())
                .sizeGB(v.getSizeGB())
                .initiatorType(v.getInitiatorType())
                .status(v.getStatus())
                .errorMessage(v.getErrorMessage())
                .iscsiConnection(iscsiInfo)
                .esxiIntegration(esxiInfo)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildVolumeKey(Long projectId, String name) {
        return "proj-" + projectId + "-" + name;
    }

    private String resolveDatastoreName(
            CreateBlockVolumeRequest.EsxiIntegrationRequest req, String fallback) {
        return (req.getDatastoreName() != null && !req.getDatastoreName().isBlank())
                ? req.getDatastoreName()
                : fallback;
    }

    private void validateName(String name) {
        if (name == null || !name.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("Invalid block volume name");
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}