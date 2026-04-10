package com.example.demo.services;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 2222;
    private static final String USER = "vagrant";
    private static final String PRIVATE_KEY =
            "C:/vagrant/staas-vagrant/.vagrant/machines/ceph-node-1/virtualbox/private_key";
    private static final String MOUNT_PATH = "/mnt/cephfs";

    // ─── Public API ───────────────────────────────────────────────

    public void createDirectory(String name) {
        validateName(name);
        executeCommand("mkdir", "-p", MOUNT_PATH + "/" + name);
    }

    public List<String> listDirectories() {
        String output = executeCommand("ls", MOUNT_PATH);
        return Arrays.stream(output.split("\n"))
                .filter(l -> !l.isBlank())
                .collect(Collectors.toList());
    }

    public List<FileEntry> listDirectory(String name) {
        validateName(name);
        String output = executeCommand("ls", "-lh", "--time-style=iso", MOUNT_PATH + "/" + name);
        return Arrays.stream(output.split("\n"))
                .filter(l -> !l.isBlank() && !l.startsWith("total"))
                .map(this::parseLsLine)
                .filter(e -> e != null)
                .collect(Collectors.toList());
    }

    public void deleteDirectory(String name) {
        validateName(name);
        executeCommand("rm", "-rf", MOUNT_PATH + "/" + name);
    }

    public String getDirectorySize(String name) {
        validateName(name);
        String raw = executeCommand("du", "-sh", MOUNT_PATH + "/" + name).trim();
        return raw.split("\\s+")[0];
    }

    public void uploadFile(String dirName, String fileName, InputStream content) {
        validateName(dirName);
        validateFileName(fileName);
        String remotePath = MOUNT_PATH + "/" + dirName + "/" + fileName;
        Session session = null;
        ChannelSftp sftp = null;
        try {
            session = openSession();
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(5000);
            sftp.put(content, remotePath);
        } catch (Exception e) {
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        } finally {
            if (sftp != null && sftp.isConnected()) sftp.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    // ─── Inner DTO ────────────────────────────────────────────────

    public static class FileEntry {
        public String name;
        public String size;
        public String date;
        public boolean isDirectory;

        public FileEntry(String name, String size, String date, boolean isDirectory) {
            this.name = name;
            this.size = size;
            this.date = date;
            this.isDirectory = isDirectory;
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private FileEntry parseLsLine(String line) {
        try {
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 8) return null;
            boolean isDir = parts[0].startsWith("d");
            String size = parts[4];
            String date = parts[5] + " " + parts[6];
            String name = parts[7];
            return new FileEntry(name, size, date, isDir);
        } catch (Exception e) {
            return null;
        }
    }

    private void validateName(String name) {
        if (name == null || !name.matches("^[a-zA-Z0-9_\\-]+$")) {
            throw new IllegalArgumentException(
                    "Invalid name — only letters, numbers, dash, underscore allowed"
            );
        }
    }

    private void validateFileName(String fileName) {
        if (fileName == null || !fileName.matches("^[a-zA-Z0-9_\\-\\.]+$") || fileName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name: " + fileName);
        }
    }

    private String executeCommand(String... args) {
        String command = String.join(" ", args);
        Session session = null;
        ChannelExec channel = null;
        try {
            session = openSession();
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("sudo " + command);

            InputStream stdout = channel.getInputStream();
            InputStream stderr = channel.getErrStream();
            channel.connect(5000);

            String out = new String(stdout.readAllBytes());
            String err = new String(stderr.readAllBytes());
            int exit = channel.getExitStatus();

            if (exit != 0) {
                throw new RuntimeException("Command failed (exit " + exit + "): " + err.trim());
            }
            return out;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("SSH failed: " + e.getMessage(), e);
        } finally {
            if (channel != null && channel.isConnected()) channel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    private Session openSession() throws Exception {
        JSch jsch = new JSch();
        jsch.addIdentity(PRIVATE_KEY);
        Session session = jsch.getSession(USER, HOST, PORT);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(5000);
        return session;
    }
}