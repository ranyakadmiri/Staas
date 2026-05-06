package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private Ssh ssh = new Ssh();
    private Cephfs cephfs = new Cephfs();
    private Nfs nfs = new Nfs();

    @Data
    public static class Ssh {
        private String host;
        private int port;
        private String user;
        private String privateKey;
    }

    @Data
    public static class Cephfs {
        private String mountPath;
    }

    @Data
    public static class Nfs {
        private String serverIp;
        private String allowedClients;
        private String ganeshaConfigPath;
        private int exportIdStart;
    }
}