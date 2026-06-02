package com.example.demo.dto;

import lombok.Data;

/**
 * Request body for creating a block volume.
 *
 * <p>For generic iSCSI clients (Linux, Windows, cloud VMs), leave
 * {@code esxiIntegration} null. The API will return the IQN and portal
 * address the client uses to connect with open-iscsi / iSCSI Initiator.
 *
 * <p>For VMware ESXi clients, populate {@code esxiIntegration}. The API will
 * additionally rescan the ESXi host and create a VMFS datastore automatically.
 */
@Data
public class CreateBlockVolumeRequest {

    /** User-visible volume name. Must match {@code ^[a-zA-Z0-9._-]+$}. */
    private String name;

    /** Size in gigabytes. */
    private int sizeGB;

    /**
     * The iSCSI Qualified Name of the initiator that will connect to this volume.
     *
     * <p>For Linux: output of {@code cat /etc/iscsi/initiatorname.iscsi}<br>
     * For Windows: from iSCSI Initiator Properties → Configuration tab<br>
     * For ESXi: from Storage Adapters → iSCSI Software Adapter → Properties
     */
    private String initiatorIqn;

    /**
     * Optional. Populate to enable automatic VMFS datastore creation on an
     * ESXi host after the iSCSI LUN is presented.
     *
     * <p>When null, provisioning ends at {@code ISCSI_READY} and the volume
     * is ready for any generic iSCSI initiator.
     */
    private EsxiIntegrationRequest esxiIntegration;

    // ── Nested DTO ────────────────────────────────────────────────────────────

    /**
     * VMware ESXi integration parameters. When present, the provisioning
     * pipeline will continue past LUN_READY to rescan ESXi and create a
     * VMFS datastore.
     */
    @Data
    public static class EsxiIntegrationRequest {
        /**
         * Desired name of the VMFS datastore to create on the ESXi host.
         * Defaults to the volume key if null.
         */
        private String datastoreName;
    }
}