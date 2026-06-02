package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class FileShareResponse {
    private Long id;
    private Long projectId;
    private String name;
    private String status;

    // Ce que le client reçoit — commandes prêtes à l'emploi
    private MountInfo mountInfo;

    @Data
    @Builder
    public static class MountInfo {
        private String server;
        private String exportPath;
        private String nfsVersion;
        private String linuxCommand;
        private String windowsCommand;
        private String macosCommand;
    }
}
/*
@Data
@Builder
public class FileShareResponse {
    private Long id;
    private Long projectId;
    private String name;
    private String shareKey;
    private String pseudoPath;
    private String realPath;
    private String serverIp;
    private Integer exportId;
    private String status;
    private String mountTarget;
}*/