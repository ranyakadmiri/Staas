package com.example.demo.services;

import com.example.demo.config.StorageProperties;
import com.example.demo.dto.FileEntryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CephFsService {

    private final CephSshService sshService;
    private final StorageProperties properties;

    // ========================
    // PATH
    // ========================

    public String buildRealPath(String shareKey) {
        return properties.getCephfs().getMountPath() + "/" + shareKey;
    }

    // ========================
    // DIRECTORY MANAGEMENT
    // ========================

    public void createDirectory(String shareKey) {
        String path = buildRealPath(shareKey);

        sshService.executeOrThrow("sudo mkdir -p " + path);
        sshService.executeOrThrow("sudo chmod 777 " + path);
    }

    public void deleteDirectory(String shareKey) {
        String path = buildRealPath(shareKey);
        sshService.executeOrThrow("sudo rm -rf " + path);
    }

    public String getDirectorySize(String shareKey) {
        String path = buildRealPath(shareKey);

        String output = sshService.executeOrThrow(
                "sudo du -sh " + path
        ).trim();

        return output.split("\\s+")[0];
    }

    // ========================
    // LIST FILES
    // ========================

    public List<FileEntryDTO> listDirectory(String shareKey) {
        String path = buildRealPath(shareKey);

        String output = sshService.executeOrThrow(
                "sudo ls -lh --time-style=iso " + path
        );

        return Arrays.stream(output.split("\n"))
                .filter(line -> !line.isBlank() && !line.startsWith("total"))
                .map(this::parseLsLine)
                .filter(e -> e != null)
                .collect(Collectors.toList());
    }

    // ========================
    // FILE UPLOAD (SFTP)
    // ========================

    public void uploadFile(String shareKey, String fileName, InputStream content) {
        validateFileName(fileName);

        com.jcraft.jsch.Session session = null;
        com.jcraft.jsch.ChannelSftp sftp = null;

        try {
            com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
            jsch.addIdentity(properties.getSsh().getPrivateKey());

            session = jsch.getSession(
                    properties.getSsh().getUser(),
                    properties.getSsh().getHost(),
                    properties.getSsh().getPort()
            );
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(10000);

            sftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
            sftp.connect(5000);

            String remotePath = buildRealPath(shareKey) + "/" + fileName;

            sftp.put(content, remotePath);

        } catch (Exception e) {
            throw new RuntimeException("File upload failed", e);
        } finally {
            if (sftp != null) sftp.disconnect();
            if (session != null) session.disconnect();
        }
    }

    // ========================
    // PARSING
    // ========================

    private FileEntryDTO parseLsLine(String line) {
        try {
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 8) return null;

            boolean isDir = parts[0].startsWith("d");
            String size = parts[4];
            String date = parts[5] + " " + parts[6];
            String name = parts[7];

            return new FileEntryDTO(name, size, date, isDir);

        } catch (Exception e) {
            return null;
        }
    }

    // ========================
    // VALIDATION
    // ========================

    private void validateFileName(String fileName) {
        if (fileName == null ||
                !fileName.matches("^[a-zA-Z0-9._\\-]+$") ||
                fileName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }
    }
    public CephSshService getSshService() {
        return sshService;
    }
}