package com.example.demo.services;

import com.example.demo.entities.BlockVolume;
import com.example.demo.entities.BlockVolumeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Owns the pure iSCSI provisioning lifecycle: RBD image creation through
 * LUN presentation via ceph-iscsi (gwcli).
 *
 * <p>This service is intentionally unaware of ESXi. After
 * {@link #provision(BlockVolume)} completes, the volume status is
 * {@link BlockVolumeStatus#LUN_READY} and the IQN/portal are ready for
 * any iSCSI initiator (Linux, Windows, VMware, cloud VMs, etc.).
 *
 * <p>ESXi-specific steps live in {@link EsxiIntegrationService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IscsiProvisioningService {

    private static final String GATEWAY_NAME = "ceph-node-1";
    private static final String GATEWAY_IP   = "192.168.100.51";
    private static final int    LUN_POLL_ATTEMPTS = 20;
    private static final long   LUN_POLL_INTERVAL_MS = 2000;

    private final BlockStorageService blockStorageService;
    private final CephSshService      sshService;
    private final ProvisionEventService eventService;

    // ── Provisioning ─────────────────────────────────────────────────────────

    /**
     * Runs the full iSCSI provisioning pipeline for the given volume.
     * On success the volume's status is {@link BlockVolumeStatus#LUN_READY}.
     * On failure an exception is thrown; the caller is responsible for
     * marking the volume as {@link BlockVolumeStatus#ERROR}.
     */
    public void provision(BlockVolume volume) {

        // 1. RBD image
        if (!blockStorageService.volumeExists(volume.getVolumeKey())) {
            blockStorageService.createVolume(volume.getVolumeKey(), volume.getSizeGB());
        }
        eventService.updateStatus(volume, BlockVolumeStatus.RBD_CREATED);
        eventService.addEvent(volume, "RBD_CREATED", "RBD image created in pool " + volume.getPoolName(), true);

        // 2. iSCSI target
        runGwcli(volume, """
                cd /iscsi-targets
                create %s
                """.formatted(volume.getTargetIqn()));

        eventService.updateStatus(volume, BlockVolumeStatus.TARGET_CREATED);
        eventService.addEvent(volume, "TARGET_CREATED", "iSCSI target created: " + volume.getTargetIqn(), true);

        // 3. Gateway
        runGwcli(volume, """
                cd /iscsi-targets/%s/gateways
                create %s %s
                """.formatted(volume.getTargetIqn(), GATEWAY_NAME, GATEWAY_IP));

        eventService.updateStatus(volume, BlockVolumeStatus.GATEWAY_ADDED);
        eventService.addEvent(volume, "GATEWAY_ADDED", "Gateway attached: " + GATEWAY_IP, true);

        // 4. Expose RBD disk in ceph-iscsi
        runGwcli(volume, """
                cd /disks
                create pool=%s image=%s
                """.formatted(volume.getPoolName(), volume.getVolumeKey()));

        eventService.updateStatus(volume, BlockVolumeStatus.DISK_EXPOSED);
        eventService.addEvent(volume, "DISK_EXPOSED", "RBD exposed in ceph-iscsi", true);

        // 5 & 6 — only if initiatorIqn provided (ESXi path, or legacy direct attach)
        if (volume.getInitiatorIqn() != null && !volume.getInitiatorIqn().isBlank()) {

            runGwcli(volume, """
            cd /iscsi-targets/%s/hosts
            create %s
            """.formatted(volume.getTargetIqn(), volume.getInitiatorIqn()));

            eventService.addEvent(volume, "HOST_CREATED",
                    "Initiator registered: " + volume.getInitiatorIqn(), true);

            runGwcli(volume, """
            cd /iscsi-targets/%s/hosts/%s
            disk add %s/%s
            """.formatted(
                    volume.getTargetIqn(),
                    volume.getInitiatorIqn(),
                    volume.getPoolName(),
                    volume.getVolumeKey()));

            eventService.updateStatus(volume, BlockVolumeStatus.HOST_MAPPED);
            eventService.addEvent(volume, "HOST_MAPPED", "Disk mapped to initiator", true);
        }

        // 7. Wait for LUN to become active in targetcli
        waitForLunReady(volume);

        eventService.updateStatus(volume, BlockVolumeStatus.LUN_READY);
        eventService.addEvent(volume, "LUN_READY", "LUN is active on target side", true);
    }

    // ── Teardown ──────────────────────────────────────────────────────────────

    /**
     * Removes all ceph-iscsi and RBD artefacts for the given volume.
     * Individual steps are attempted independently; failures are logged as
     * warning events but do not abort the overall teardown.
     */
    public void deprovision(BlockVolume volume) {

        runGwcliSilently(volume, """
                cd /iscsi-targets/%s/hosts/%s
                disk remove %s/%s
                """.formatted(
                volume.getTargetIqn(),
                volume.getInitiatorIqn(),
                volume.getPoolName(),
                volume.getVolumeKey()));

        runGwcliSilently(volume, """
                cd /iscsi-targets/%s/hosts
                delete %s
                """.formatted(volume.getTargetIqn(), volume.getInitiatorIqn()));

        runGwcliSilently(volume, """
                cd /disks
                delete %s/%s
                """.formatted(volume.getPoolName(), volume.getVolumeKey()));

        runGwcliSilently(volume, """
                cd /iscsi-targets
                delete %s
                """.formatted(volume.getTargetIqn()));

        if (blockStorageService.volumeExists(volume.getVolumeKey())) {
            blockStorageService.deleteVolume(volume.getVolumeKey());
        }
    }

    // ── Disk append (used by appendDiskToVolume) ──────────────────────────────

    /**
     * Creates a new RBD image and exposes it as a new iSCSI LUN under the
     * same initiator as {@code existingVolume}. Does not create a new datastore.
     *
     * @param existingVolume the volume whose initiator IQN will be reused
     * @param newVolumeKey   RBD image name / iSCSI target suffix for the new disk
     * @param targetIqn      full IQN for the new target
     * @param sizeGB         size of the new disk
     */
    public void provisionAdditionalDisk(
            BlockVolume existingVolume, String newVolumeKey, String targetIqn, int sizeGB) {

        if (!blockStorageService.volumeExists(newVolumeKey)) {
            blockStorageService.createVolume(newVolumeKey, sizeGB);
        }

        runGwcli(existingVolume, """
                cd /iscsi-targets
                create %s
                """.formatted(targetIqn));

        runGwcli(existingVolume, """
                cd /iscsi-targets/%s/gateways
                create %s %s
                """.formatted(targetIqn, GATEWAY_NAME, GATEWAY_IP));

        runGwcli(existingVolume, """
                cd /disks
                create pool=%s image=%s
                """.formatted(blockStorageService.getPool(), newVolumeKey));

        runGwcli(existingVolume, """
                cd /iscsi-targets/%s/hosts
                create %s
                """.formatted(targetIqn, existingVolume.getInitiatorIqn()));

        runGwcli(existingVolume, """
                cd /iscsi-targets/%s/hosts/%s
                disk add %s/%s
                """.formatted(targetIqn, existingVolume.getInitiatorIqn(),
                blockStorageService.getPool(), newVolumeKey));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void waitForLunReady(BlockVolume volume) {
        for (int i = 0; i < LUN_POLL_ATTEMPTS; i++) {
            String out = sshService.executeBash("sudo targetcli ls");
            if (out.contains(volume.getVolumeKey())) {
                return;
            }
            sleep(LUN_POLL_INTERVAL_MS);
        }
        throw new RuntimeException("LUN did not become active in targetcli after "
                + LUN_POLL_ATTEMPTS + " attempts");
    }

    private void runGwcli(BlockVolume volume, String innerCommand) {
        String script = """
                gwcli <<'EOF'
                %s
                exit
                EOF
                """.formatted(innerCommand);

        String output = sshService.executeBash(script);
        eventService.addEvent(volume, "GWCLI", output, true);
    }

    /**
     * Runs a gwcli command and swallows any exception, logging it as a
     * warning event. Used during teardown where partial failure is acceptable.
     */
    private void runGwcliSilently(BlockVolume volume, String innerCommand) {
        try {
            runGwcli(volume, innerCommand);
        } catch (Exception e) {
            log.warn("[gwcli] Teardown step failed (ignored): {}", e.getMessage());
            eventService.addEvent(volume, "GWCLI_WARN", e.getMessage(), false);
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
    // ADD these two methods to your existing IscsiProvisioningService.java
// Place them after provisionAdditionalDisk() and before the private helpers section

    /**
     * Registers an initiator on an existing target and maps the disk to it.
     * Called by AttachService when a client agent requests a volume attachment.
     *
     * The target, gateway, and disk are already configured on Ceph —
     * this only adds the host (initiator) entry and the disk mapping.
     *
     * @param volume        the volume to attach
     * @param initiatorIqn  the client VM's iSCSI initiator IQN (from agents table)
     */
    public void registerInitiatorAndMapDisk(BlockVolume volume, String initiatorIqn) {

        // Register the initiator on the target
        runGwcli(volume, """
                cd /iscsi-targets/%s/hosts
                create %s
                """.formatted(volume.getTargetIqn(), initiatorIqn));

        log.info("[gwcli] Registered initiator {} on target {}",
                initiatorIqn, volume.getTargetIqn());

        // Map the disk to the initiator
        runGwcli(volume, """
                cd /iscsi-targets/%s/hosts/%s
                disk add %s/%s
                """.formatted(
                volume.getTargetIqn(),
                initiatorIqn,
                volume.getPoolName(),
                volume.getVolumeKey()));

        log.info("[gwcli] Disk {}/{} mapped to initiator {}",
                volume.getPoolName(), volume.getVolumeKey(), initiatorIqn);
    }

    /**
     * Removes the disk mapping and deletes the initiator host entry from the target.
     * Called by AttachService during detach, after the agent has logged out.
     *
     * @param volume        the volume being detached
     * @param initiatorIqn  the client VM's iSCSI initiator IQN
     */
    public void unmapInitiator(BlockVolume volume, String initiatorIqn) {

        runGwcliSilently(volume, """
                cd /iscsi-targets/%s/hosts/%s
                disk remove %s/%s
                """.formatted(
                volume.getTargetIqn(),
                initiatorIqn,
                volume.getPoolName(),
                volume.getVolumeKey()));

        runGwcliSilently(volume, """
                cd /iscsi-targets/%s/hosts
                delete %s
                """.formatted(volume.getTargetIqn(), initiatorIqn));

        log.info("[gwcli] Initiator {} removed from target {}",
                initiatorIqn, volume.getTargetIqn());
    }
}