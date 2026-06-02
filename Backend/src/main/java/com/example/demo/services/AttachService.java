package com.example.demo.services;

import com.example.demo.entities.Agent;
import com.example.demo.entities.BlockVolume;
import com.example.demo.entities.BlockVolumeStatus;
import com.example.demo.repositories.AgentRepository;
import com.example.demo.repositories.BlockVolumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handles volume attach and detach for generic iSCSI clients using the agent.
 *
 * <p>Full flow for ATTACH:
 * <ol>
 *   <li>Look up the agent → get its initiatorIqn</li>
 *   <li>Run gwcli on Ceph: register the initiator + map the disk to it</li>
 *   <li>Enqueue an ATTACH command for the agent</li>
 *   <li>Agent picks it up within 10s, runs iscsiadm, disk appears on VM</li>
 * </ol>
 *
 * <p>Full flow for DETACH:
 * <ol>
 *   <li>Look up the agent → get its initiatorIqn</li>
 *   <li>Enqueue a DETACH command for the agent</li>
 *   <li>Agent runs iscsiadm --logout</li>
 *   <li>Run gwcli: remove disk mapping + delete host from target</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachService {

    private static final String GATEWAY_IP   = "192.168.100.51";
    private static final int    GATEWAY_PORT = 3260;

    private final AgentRepository        agentRepository;
    private final BlockVolumeRepository  volumeRepository;
    private final IscsiProvisioningService iscsiProvisioning;
    private final AgentService           agentService;
    private final ProvisionEventService  eventService;

    // ── Attach ────────────────────────────────────────────────────────────────

    /**
     * Attaches a volume to a client VM via the agent.
     *
     * @param projectId project that owns the volume
     * @param volumeName volume to attach
     * @param agentId    registered agent on the client VM
     */
    public void attach(Long projectId, String volumeName, Long agentId) {

        BlockVolume volume = findVolume(projectId, volumeName);
        Agent agent = findAgent(agentId, projectId);

        log.info("[Attach] volume={} agent={} initiator={}",
                volumeName, agentId, agent.getInitiatorIqn());

        // 1. Register the initiator on the ceph-iscsi target and map the disk
        //    (this is the gwcli work that was previously done manually)
        iscsiProvisioning.registerInitiatorAndMapDisk(
                volume, agent.getInitiatorIqn());

        eventService.addEvent(volume, "HOST_MAPPED",
                "Initiator " + agent.getInitiatorIqn() + " mapped via agent attach", true);

        // 2. Enqueue the ATTACH command — agent picks it up and runs iscsiadm
        agentService.enqueueAttach(
                agentId,
                volume.getTargetIqn(),
                GATEWAY_IP + ":" + GATEWAY_PORT);

        eventService.addEvent(volume, "ATTACH_QUEUED",
                "ATTACH command queued for agent " + agentId, true);

        // 3. Update volume status
        volume.setStatus(BlockVolumeStatus.ISCSI_READY);
        volumeRepository.save(volume);

        log.info("[Attach] command queued — agent will connect within {}s", 10);
    }

    // ── Detach ────────────────────────────────────────────────────────────────

    /**
     * Detaches a volume from a client VM.
     * The agent disconnects first, then we clean up the ceph-iscsi host mapping.
     *
     * @param projectId  project that owns the volume
     * @param volumeName volume to detach
     * @param agentId    registered agent on the client VM
     */
    public void detach(Long projectId, String volumeName, Long agentId) {

        BlockVolume volume = findVolume(projectId, volumeName);
        Agent agent = findAgent(agentId, projectId);

        log.info("[Detach] volume={} agent={} initiator={}",
                volumeName, agentId, agent.getInitiatorIqn());

        // 1. Tell the agent to logout first (so the OS releases the device cleanly)
        agentService.enqueueDetach(
                agentId,
                volume.getTargetIqn(),
                GATEWAY_IP + ":" + GATEWAY_PORT);

        eventService.addEvent(volume, "DETACH_QUEUED",
                "DETACH command queued for agent " + agentId, true);

        // 2. Remove the host mapping from ceph-iscsi
        //    (the agent will have logged out before the next gwcli command matters)
        iscsiProvisioning.unmapInitiator(volume, agent.getInitiatorIqn());

        eventService.addEvent(volume, "HOST_UNMAPPED",
                "Initiator " + agent.getInitiatorIqn() + " removed from target", true);

        log.info("[Detach] done — agent will disconnect within {}s", 10);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BlockVolume findVolume(Long projectId, String volumeName) {
        return volumeRepository.findByProjectIdAndName(projectId, volumeName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Volume not found: " + volumeName));
    }

    private Agent findAgent(Long agentId, Long projectId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Agent not found: " + agentId));

        if (!agent.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException(
                    "Agent " + agentId + " does not belong to project " + projectId);
        }

        return agent;
    }
}