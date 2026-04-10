package com.example.demo.services;

import com.example.demo.dto.IscsiVolumeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IscsiService {

    private final BlockStorageService blockStorageService;
    private final CephSshService sshService;

    // Change these to match your lab
    private static final String GATEWAY_NAME = "ceph-node-1";
    private static final String GATEWAY_IP = "192.168.100.51";
    private static final String IQN_PREFIX = "iqn.2026-04.com.staas:";

    /**
     * IMPORTANT:
     * synchronize so you do NOT run multiple gwcli changes in parallel.
     */
    public synchronized String createIscsiVolume(IscsiVolumeDTO dto) {
        String volumeName = dto.getName();
        String targetIqn = IQN_PREFIX + volumeName;
        String initiatorIqn = dto.getInitiatorIqn();
        String pool = blockStorageService.getPool();

        // 1) Create RBD volume if it does not exist
        if (!blockStorageService.volumeExists(volumeName)) {
            blockStorageService.createVolume(volumeName, dto.getSizeGB());
        }

        // 2) Configure gwcli
        String script = """
                gwcli <<'EOF'
                cd /iscsi-targets
                create %s
                cd %s/gateways
                create %s %s
                cd /disks
                create pool=%s image=%s
                cd /iscsi-targets/%s/hosts
                create %s
                cd %s
                disk add %s/%s
                exit
                EOF
                """.formatted(
                targetIqn,
                targetIqn,
                GATEWAY_NAME, GATEWAY_IP,
                pool, volumeName,
                targetIqn,
                initiatorIqn,
                initiatorIqn,
                pool, volumeName
        );

        String output = sshService.executeBash(script);

        return """
                iSCSI volume created successfully.
                Volume: %s
                Target IQN: %s
                Gateway IP: %s
                Initiator IQN: %s
                """.formatted(volumeName, targetIqn, GATEWAY_IP, initiatorIqn) + "\n" + output;
    }

    public synchronized String deleteIscsiVolume(String volumeName, String initiatorIqn) {
        String targetIqn = IQN_PREFIX + volumeName;
        String pool = blockStorageService.getPool();

        String script = """
                gwcli <<'EOF'
                cd /iscsi-targets/%s/hosts/%s
                disk remove %s/%s
                cd /iscsi-targets/%s/hosts
                delete %s
                cd /disks
                delete %s/%s
                cd /iscsi-targets
                delete %s
                exit
                EOF
                """.formatted(
                targetIqn, initiatorIqn,
                pool, volumeName,
                targetIqn,
                initiatorIqn,
                pool, volumeName,
                targetIqn
        );

        String output = sshService.executeBash(script);

        if (blockStorageService.volumeExists(volumeName)) {
            blockStorageService.deleteVolume(volumeName);
        }

        return """
                iSCSI volume deleted successfully.
                Volume: %s
                Target IQN: %s
                Initiator IQN: %s
                """.formatted(volumeName, targetIqn, initiatorIqn) + "\n" + output;
    }

    public synchronized String listTargets() {
        String script = """
                gwcli <<'EOF'
                cd /iscsi-targets
                ls
                exit
                EOF
                """;
        return sshService.executeBash(script);
    }

    public synchronized String listDisks() {
        String script = """
                gwcli <<'EOF'
                cd /disks
                ls
                exit
                EOF
                """;
        return sshService.executeBash(script);
    }

    public String buildEsxiHelp(String volumeName) {
        String targetIqn = IQN_PREFIX + volumeName;
        return """
                ESXi connection info:
                - Target portal IP: %s
                - Port: 3260
                - Target IQN: %s

                In ESXi:
                1. Storage Adapters
                2. Select Software iSCSI Adapter
                3. Add Dynamic Discovery: %s:3260
                4. Or add Static Target:
                   - IQN: %s
                   - Address: %s
                   - Port: 3260
                5. Rescan adapter
                6. Create datastore on discovered disk
                """.formatted(GATEWAY_IP, targetIqn, GATEWAY_IP, targetIqn, GATEWAY_IP);
    }
}
