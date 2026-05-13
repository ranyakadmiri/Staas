package com.example.demo.services;

import com.example.demo.dto.BlockVolumeResponse;
import com.example.demo.dto.CreateBlockVolumeRequest;
import com.example.demo.entities.BlockVolume;
import com.example.demo.entities.BlockVolumeStatus;
import com.example.demo.repositories.BlockVolumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BlockVolumeService {

    private static final String GATEWAY_NAME = "ceph-node-1";
    private static final String GATEWAY_IP = "192.168.100.51";
    private static final String IQN_PREFIX = "iqn.2026-04.com.staas:";

    private final BlockVolumeRepository repository;
    private final ProvisionEventService eventService;
    private final BlockStorageService blockStorageService;
    private final CephSshService sshService;
    private final EsxiService esxiService;

    public synchronized BlockVolumeResponse createVolume(Long projectId, CreateBlockVolumeRequest dto) {
        validateName(dto.getName());

        if (repository.existsByProjectIdAndName(projectId, dto.getName())) {
            throw new IllegalArgumentException("Block volume already exists in this project");
        }

        String volumeKey = buildVolumeKey(projectId, dto.getName());
        String targetIqn = IQN_PREFIX + volumeKey;
        String datastoreName = volumeKey;

        BlockVolume volume = BlockVolume.builder()
                .projectId(projectId)
                .name(dto.getName())
                .volumeKey(volumeKey)
                .sizeGB(dto.getSizeGB())
                .poolName(blockStorageService.getPool())
                .targetIqn(targetIqn)
                .initiatorIqn(dto.getInitiatorIqn())
                .gatewayIp(GATEWAY_IP)
                .datastoreName(datastoreName)
                .status(BlockVolumeStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        repository.save(volume);
        eventService.addEvent(volume, "START", "Starting block volume provisioning", true);

        try {
            // IMPORTANT: snapshot ESXi disks BEFORE exposing the new LUN
            Set<String> disksBefore = new HashSet<>(esxiService.listDisks());

            eventService.addEvent(volume, "ESXI_SNAPSHOT", "Captured ESXi disks before provisioning", true);

            if (!blockStorageService.volumeExists(volumeKey)) {
                blockStorageService.createVolume(volumeKey, dto.getSizeGB());
            }

            eventService.updateStatus(volume, BlockVolumeStatus.RBD_CREATED);
            eventService.addEvent(volume, "RBD_CREATED", "RBD image created in pool rbd", true);

            runGwcli(volume, """
                cd /iscsi-targets
                create %s
                """.formatted(targetIqn));

            eventService.updateStatus(volume, BlockVolumeStatus.TARGET_CREATED);
            eventService.addEvent(volume, "TARGET_CREATED", "iSCSI target created: " + targetIqn, true);

            runGwcli(volume, """
                cd /iscsi-targets/%s/gateways
                create %s %s
                """.formatted(targetIqn, GATEWAY_NAME, GATEWAY_IP));

            eventService.updateStatus(volume, BlockVolumeStatus.GATEWAY_ADDED);
            eventService.addEvent(volume, "GATEWAY_ADDED", "Gateway attached: " + GATEWAY_IP, true);

            runGwcli(volume, """
                cd /disks
                create pool=%s image=%s
                """.formatted(volume.getPoolName(), volumeKey));

            eventService.updateStatus(volume, BlockVolumeStatus.DISK_EXPOSED);
            eventService.addEvent(volume, "DISK_EXPOSED", "RBD exposed in ceph-iscsi", true);

            runGwcli(volume, """
                cd /iscsi-targets/%s/hosts
                create %s
                """.formatted(targetIqn, volume.getInitiatorIqn()));

            eventService.addEvent(volume, "HOST_CREATED", "ESXi initiator registered", true);

            runGwcli(volume, """
                cd /iscsi-targets/%s/hosts/%s
                disk add %s/%s
                """.formatted(
                    targetIqn,
                    volume.getInitiatorIqn(),
                    volume.getPoolName(),
                    volumeKey
            ));

            eventService.updateStatus(volume, BlockVolumeStatus.HOST_MAPPED);
            eventService.addEvent(volume, "HOST_MAPPED", "Disk mapped to ESXi initiator", true);

            waitForLunReady(volume);

            eventService.updateStatus(volume, BlockVolumeStatus.LUN_READY);
            eventService.addEvent(volume, "LUN_READY", "LUN is active on target side", true);
            //hedhy zedtha
            //eventService.addEvent(volume, "WAIT_ESXI", "Waiting for ESXi to discover LUN", true);
            // Correct detection: compare ESXi disks after provisioning with snapshot before provisioning
           String newDisk = esxiService.detectNewDisk(disksBefore);
           // String newDisk = esxiService.detectDiskByIqn(targetIqn);
            volume.setEsxiDiskCanonical(newDisk);
            repository.save(volume);

            eventService.updateStatus(volume, BlockVolumeStatus.ESXI_RESCANNED);
            eventService.addEvent(volume, "ESXI_RESCANNED", "ESXi detected disk: " + newDisk, true);

            esxiService.createDatastore(volume.getDatastoreName(), newDisk);

            eventService.updateStatus(volume, BlockVolumeStatus.DATASTORE_CREATED);
            eventService.addEvent(volume, "DATASTORE_CREATED", "VMFS datastore created on ESXi", true);

            volume.setStatus(BlockVolumeStatus.READY);
            volume.setUpdatedAt(LocalDateTime.now());
            repository.save(volume);

            eventService.addEvent(volume, "READY", "Provisioning finished successfully", true);

            return toResponse(volume);

        } catch (Exception e) {
            eventService.fail(volume, "ERROR", e.getMessage());
            throw new RuntimeException("Failed to provision block volume: " + e.getMessage(), e);
        }
    }

    public synchronized void deleteVolume(Long projectId, String name) {
        BlockVolume volume = repository.findByProjectIdAndName(projectId, name)
                .orElseThrow(() -> new IllegalArgumentException("Block volume not found"));

        volume.setStatus(BlockVolumeStatus.DELETING);
        volume.setUpdatedAt(LocalDateTime.now());
        repository.save(volume);

        eventService.addEvent(volume, "DELETING", "Deleting block volume", true);

        try {
            try {
                esxiService.deleteDatastore(volume.getDatastoreName());
                eventService.addEvent(volume, "DATASTORE_DELETED", "ESXi datastore deleted", true);
            } catch (Exception e) {
                eventService.addEvent(volume, "DATASTORE_DELETE_WARNING", e.getMessage(), false);
            }

            try {
                runGwcli(volume, """
                    cd /iscsi-targets/%s/hosts/%s
                    disk remove %s/%s
                    """.formatted(
                        volume.getTargetIqn(),
                        volume.getInitiatorIqn(),
                        volume.getPoolName(),
                        volume.getVolumeKey()
                ));
            } catch (Exception ignored) {}

            try {
                runGwcli(volume, """
                    cd /iscsi-targets/%s/hosts
                    delete %s
                    """.formatted(volume.getTargetIqn(), volume.getInitiatorIqn()));
            } catch (Exception ignored) {}

            try {
                runGwcli(volume, """
                    cd /disks
                    delete %s/%s
                    """.formatted(volume.getPoolName(), volume.getVolumeKey()));
            } catch (Exception ignored) {}

            try {
                runGwcli(volume, """
                    cd /iscsi-targets
                    delete %s
                    """.formatted(volume.getTargetIqn()));
            } catch (Exception ignored) {}

            if (blockStorageService.volumeExists(volume.getVolumeKey())) {
                blockStorageService.deleteVolume(volume.getVolumeKey());
            }

            volume.setStatus(BlockVolumeStatus.DELETED);
            volume.setUpdatedAt(LocalDateTime.now());
            repository.save(volume);

            eventService.addEvent(volume, "DELETED", "Block volume deleted", true);

        } catch (Exception e) {
            eventService.fail(volume, "DELETE_ERROR", e.getMessage());
            throw new RuntimeException("Failed to delete block volume: " + e.getMessage(), e);
        }
    }

    public List<BlockVolumeResponse> listProjectVolumes(Long projectId) {
        return repository.findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BlockVolumeResponse getVolume(Long projectId, String name) {
        BlockVolume volume = repository.findByProjectIdAndName(projectId, name)
                .orElseThrow(() -> new IllegalArgumentException("Block volume not found"));

        return toResponse(volume);
    }

    private void waitForLunReady(BlockVolume volume) {
        for (int i = 0; i < 20; i++) {
            String out = sshService.executeBash("sudo targetcli ls");

            if (out.contains(volume.getVolumeKey())) {
                return;
            }

            sleep(2000);
        }

        throw new RuntimeException("LUN did not become active in targetcli");
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

    private String buildVolumeKey(Long projectId, String name) {
        return "proj-" + projectId + "-" + name;
    }

    private void validateName(String name) {
        if (name == null || !name.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("Invalid block volume name");
        }
    }

    private BlockVolumeResponse toResponse(BlockVolume v) {
        return BlockVolumeResponse.builder()
                .id(v.getId())
                .projectId(v.getProjectId())
                .name(v.getName())
                .volumeKey(v.getVolumeKey())
                .sizeGB(v.getSizeGB())
                .targetIqn(v.getTargetIqn())
                .initiatorIqn(v.getInitiatorIqn())
                .gatewayIp(v.getGatewayIp())
                .datastoreName(v.getDatastoreName())
                .esxiDiskCanonical(v.getEsxiDiskCanonical())
                .status(v.getStatus())
                .errorMessage(v.getErrorMessage())
                .build();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
   public synchronized BlockVolumeResponse appendDiskToVolume(
            Long projectId, String existingVolumeName, CreateBlockVolumeRequest dto) {

        // Load the existing volume (for its datastore name)
        BlockVolume existing = repository.findByProjectIdAndName(projectId, existingVolumeName)
                .orElseThrow(() -> new IllegalArgumentException("Base volume not found"));

        validateName(dto.getName());

        String volumeKey = buildVolumeKey(projectId, dto.getName());
        String targetIqn = IQN_PREFIX + volumeKey;

        // Snapshot disks BEFORE exposing the new LUN
        Set<String> disksBefore = new HashSet<>(esxiService.listDisks());

        // 1. Create new RBD image
        if (!blockStorageService.volumeExists(volumeKey)) {
            blockStorageService.createVolume(volumeKey, dto.getSizeGB());
        }

        // 2. Create iSCSI target + gateway + disk + host mapping
        runGwcli(existing, """
        cd /iscsi-targets
        create %s
        """.formatted(targetIqn));

        runGwcli(existing, """
        cd /iscsi-targets/%s/gateways
        create %s %s
        """.formatted(targetIqn, GATEWAY_NAME, GATEWAY_IP));

        runGwcli(existing, """
        cd /disks
        create pool=%s image=%s
        """.formatted(blockStorageService.getPool(), volumeKey));

        runGwcli(existing, """
        cd /iscsi-targets/%s/hosts
        create %s
        """.formatted(targetIqn, existing.getInitiatorIqn()));

        runGwcli(existing, """
        cd /iscsi-targets/%s/hosts/%s
        disk add %s/%s
        """.formatted(targetIqn, existing.getInitiatorIqn(),
                blockStorageService.getPool(), volumeKey));

        // 3. Wait for LUN, then detect new disk on ESXi
        // (reuse waitForLunReady with a temporary volume object if needed)
        sleep(5000);
        esxiService.rescan();
        sleep(3000);

        String newDisk = esxiService.detectNewDisk(disksBefore);

        // 4. Append disk as VMFS extent — NOT a new datastore
        esxiService.appendDiskToDatastore(existing.getDatastoreName(), newDisk);

        // Optionally: persist the new extent as a separate entity or just return
        return toResponse(existing);
    }
}