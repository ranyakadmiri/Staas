package com.example.demo.entities;

public enum BlockVolumeStatus {
    PENDING,
    RBD_CREATED,
    TARGET_CREATED,
    GATEWAY_ADDED,
    DISK_EXPOSED,
    HOST_MAPPED,
    LUN_READY,
    ESXI_RESCANNED,
    DATASTORE_CREATED,
    READY,
    DELETING,
    DELETED,
    ERROR
}