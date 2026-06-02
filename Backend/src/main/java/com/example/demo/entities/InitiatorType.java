package com.example.demo.entities;

/**
 * Describes which type of iSCSI initiator this volume is provisioned for.
 *
 * <ul>
 *   <li>{@code GENERIC_ISCSI} – any Linux/Windows/cloud VM using open-iscsi or
 *       the Windows iSCSI Initiator. Provisioning ends at {@link BlockVolumeStatus#ISCSI_READY}.</li>
 *   <li>{@code VMWARE_ESXI}   – an ESXi host; provisioning continues to create a
 *       VMFS datastore and ends at {@link BlockVolumeStatus#READY}.</li>
 * </ul>
 */
public enum InitiatorType {
    GENERIC_ISCSI,
    VMWARE_ESXI
}