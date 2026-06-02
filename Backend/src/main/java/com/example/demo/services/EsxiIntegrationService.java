package com.example.demo.services;

import com.example.demo.entities.BlockVolume;
import com.example.demo.entities.BlockVolumeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles the optional VMware ESXi integration step that runs after a LUN has
 * been presented by {@link IscsiProvisioningService}.
 *
 * <p>This service is intentionally isolated from the core iSCSI path. It is
 * only invoked when the user explicitly requests ESXi integration via
 * {@code CreateBlockVolumeRequest.esxiIntegration}.
 *
 * <p>All existing {@link EsxiService} and {@link EsxiSshService} logic is
 * preserved unchanged — this service is purely a coordination layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EsxiIntegrationService {

    private final EsxiService           esxiService;
    private final ProvisionEventService  eventService;

    // ── Pre-provisioning ──────────────────────────────────────────────────────

    /**
     * Snapshots the current set of disks visible on the ESXi host.
     * Call this <em>before</em> exposing any new LUN so that
     * {@link #detectAndRegisterNewDisk(BlockVolume, Set)} can find the delta.
     */
    public Set<String> snapshotDisks() {
        return new HashSet<>(esxiService.listDisks());
    }

    // ── Post-provisioning ─────────────────────────────────────────────────────

    /**
     * Rescans the ESXi host, detects the newly presented LUN, persists its
     * canonical name on the volume, and advances the status to
     * {@link BlockVolumeStatus#ESXI_RESCANNED}.
     *
     * @param volume      the volume that was just provisioned via iSCSI
     * @param disksBefore disk snapshot captured before LUN presentation
     * @return canonical disk name (e.g. {@code naa.600...})
     */
    public String detectAndRegisterNewDisk(BlockVolume volume, Set<String> disksBefore) {
        String newDisk = esxiService.detectNewDisk(disksBefore);

        log.info("[ESXi] Detected new disk {} for volume {}", newDisk, volume.getVolumeKey());
        eventService.updateStatus(volume, BlockVolumeStatus.ESXI_RESCANNED);
        eventService.addEvent(volume, "ESXI_RESCANNED", "ESXi detected disk: " + newDisk, true);

        return newDisk;
    }

    /**
     * Creates a VMFS datastore on the ESXi host using the given canonical disk.
     * Advances status to {@link BlockVolumeStatus#DATASTORE_CREATED}.
     */
    public void createDatastore(BlockVolume volume, String diskCanonical) {
        esxiService.createDatastore(volume.getDatastoreName(), diskCanonical);

        eventService.updateStatus(volume, BlockVolumeStatus.DATASTORE_CREATED);
        eventService.addEvent(volume, "DATASTORE_CREATED", "VMFS datastore created: " + volume.getDatastoreName(), true);
    }

    /**
     * Attempts to delete the VMFS datastore associated with the given volume.
     * Failure is treated as a warning (non-fatal) to allow teardown to continue.
     */
    public void deleteDatastore(BlockVolume volume) {
        try {
            esxiService.deleteDatastore(volume.getDatastoreName());
            eventService.addEvent(volume, "DATASTORE_DELETED", "ESXi datastore deleted", true);
        } catch (Exception e) {
            log.warn("[ESXi] Could not delete datastore {}: {}", volume.getDatastoreName(), e.getMessage());
            eventService.addEvent(volume, "DATASTORE_DELETE_WARNING", e.getMessage(), false);
        }
    }

    // ── Disk append ───────────────────────────────────────────────────────────

    /**
     * Detects a new LUN and appends it as a VMFS extent to an existing
     * datastore rather than creating a new one.
     *
     * @param existingVolume  volume whose datastore will be expanded
     * @param disksBefore     disk snapshot captured before LUN presentation
     */
    public void appendExtentToDatastore(BlockVolume existingVolume, Set<String> disksBefore) {
        esxiService.rescan();
        sleep(3000);

        String newDisk = esxiService.detectNewDisk(disksBefore);

        log.info("[ESXi] Appending disk {} to datastore {}", newDisk, existingVolume.getDatastoreName());
        esxiService.appendDiskToDatastore(existingVolume.getDatastoreName(), newDisk);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}