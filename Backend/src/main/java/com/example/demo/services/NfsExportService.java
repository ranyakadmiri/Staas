package com.example.demo.services;

import com.example.demo.config.StorageProperties;
import com.example.demo.entities.FileShare;
import com.example.demo.repositories.FileShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NfsExportService {

    private final StorageProperties properties;
    private final CephSshService sshService;
    private final FileShareRepository repository;

    // ========================
    // EXPORT ID
    // ========================

    public int nextExportId() {
        return repository.findTopByOrderByExportIdDesc()
                .map(f -> f.getExportId() + 1)
                .orElse(properties.getNfs().getExportIdStart());
    }

    // ========================
    // CREATE EXPORT
    // ========================
    public synchronized void createExport(FileShare share) {

        String exportBlock = buildExportBlock(share);

        String script = String.format("""
        set -e

        echo "%s" | sudo tee -a %s > /dev/null

        sudo systemctl reload nfs-ganesha
    """,
                exportBlock.replace("\"", "\\\""),
                properties.getNfs().getGaneshaConfigPath()
        );

        System.out.println("SCRIPT:\n" + script);

        sshService.executeOrThrow(script);
    }
    // ========================
    // DELETE EXPORT
    // ========================

    public synchronized void deleteExport(FileShare share) {

        String script = """
                set -e

                CONFIG="%s"
                EXPORT_ID=%d

                awk '
                BEGIN { skip=0 }
                /EXPORT *{/ { block=$0; skip=0 }
                /Export_Id *= *%d;/ { skip=1 }
                skip==0 { print }
                /}/ { skip=0 }
                ' "$CONFIG" > "$CONFIG.tmp"

                mv "$CONFIG.tmp" "$CONFIG"

                systemctl reload nfs-ganesha
                """.formatted(
                properties.getNfs().getGaneshaConfigPath(),
                share.getExportId(),
                share.getExportId()
        );

        sshService.executeBash(script);
    }

    // ========================
    // CHECK EXPORT EXISTS
    // ========================

    private boolean exportExists(int exportId) {
        try {
            String output = sshService.executeOrThrow(
                    "grep -q 'Export_Id = " + exportId + ";' " +
                            properties.getNfs().getGaneshaConfigPath() +
                            " && echo FOUND || echo NOT_FOUND"
            );

            return output.contains("FOUND");

        } catch (Exception e) {
            return false;
        }
    }

    // ========================
    // BUILD EXPORT BLOCK
    // ========================

    private String buildExportBlock(FileShare share) {

        return """
                EXPORT {
                    Export_Id = %d;

                    Path = "%s";
                    Pseudo = "%s";

                    Access_Type = RW;
                    Squash = No_Root_Squash;

                    Protocols = 4;
                    Transports = TCP;
                    SecType = sys;

                    CLIENT {
                        Clients = %s;
                        Access_Type = RW;
                    }

                    FSAL {
                        Name = VFS;
                    }
                }
                """.formatted(
                share.getExportId(),
                share.getRealPath(),
                share.getPseudoPath(),
                properties.getNfs().getAllowedClients()
        );
    }

    // ========================
    // SAFE BASH STRING
    // ========================

    private String escapeForBash(String input) {
        return input.replace("\"", "\\\"")
                .replace("$", "\\$")
                .replace("`", "\\`");
    }
}