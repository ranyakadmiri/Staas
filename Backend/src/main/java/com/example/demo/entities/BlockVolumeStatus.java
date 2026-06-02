package com.example.demo.entities;

public enum BlockVolumeStatus {

    // ── Generic iSCSI lifecycle ──────────────────────────────────────────────
    PENDING,
    RBD_CREATED,
    TARGET_CREATED,
    GATEWAY_ADDED,
    DISK_EXPOSED,
    HOST_MAPPED,
    LUN_READY,

    /**
     * Terminal success state for generic iSCSI clients (Linux, Windows, etc.).
     * The volume is fully provisioned and the IQN/portal are ready to hand back.
     */
    ISCSI_READY,

    // ── ESXi-specific continuation (optional integration) ───────────────────
    ESXI_RESCANNED,
    DATASTORE_CREATED,

    /**
     * Terminal success state when the ESXi datastore integration is enabled.
     */
    READY,

    // ── Teardown ─────────────────────────────────────────────────────────────
    DELETING,
    DELETED,
    ERROR
}