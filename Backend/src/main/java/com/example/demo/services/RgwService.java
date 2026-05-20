package com.example.demo.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
@Service
@Slf4j
public class RgwService {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 2222;
    private static final String USER = "vagrant";
    private static final String PRIVATE_KEY = "C:/vagrant/staas-vagrant/.vagrant/machines/ceph-node-1/virtualbox/private_key";

    public Map<String, String> createRgwUser(String uid) {
        String command = "sudo cephadm shell -- radosgw-admin user create " +
                "--uid=" + uid + " --display-name=" + uid + " --format=json";

        String output = executeCommand(command);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(output);
            String accessKey = json.get("keys").get(0).get("access_key").asText();
            String secretKey = json.get("keys").get(0).get("secret_key").asText();

            Map<String, String> result = new HashMap<>();
            result.put("accessKey", accessKey);
            result.put("secretKey", secretKey);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse RGW user response", e);
        }
    }

    // ── NEW: apply bucket quota to Ceph ──────────────────────────────────────

    public void applyBucketQuota(String uid, String bucketName, long maxSizeBytes, long maxObjects) {
        // Step 1: set the values
        String setCmd = String.format(
                "sudo cephadm shell -- radosgw-admin quota set --uid=%s --bucket=%s " +
                        "--quota-scope=bucket --max-size=%d --max-objects=%d",
                uid, bucketName, maxSizeBytes, maxObjects
        );
        executeCommand(setCmd);

        // Step 2: enable the quota (separate command in radosgw-admin)
        String enableCmd = String.format(
                "sudo cephadm shell -- radosgw-admin quota enable --uid=%s --bucket=%s --quota-scope=bucket",
                uid, bucketName
        );
        executeCommand(enableCmd);
    }

    public void disableBucketQuota(String uid, String bucketName) {
        String cmd = String.format(
                "sudo cephadm shell -- radosgw-admin quota disable --uid=%s --bucket=%s --quota-scope=bucket",
                uid, bucketName
        );
        executeCommand(cmd);
    }

    // ── shared SSH helper ─────────────────────────────────────────────────────

    private String executeCommand(String command) {
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(PRIVATE_KEY);

            Session session = jsch.getSession(USER, HOST, PORT);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            InputStream inputStream = channel.getInputStream();
            channel.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) output.append(line);

            channel.disconnect();
            session.disconnect();

            return output.toString();
        } catch (Exception e) {
            throw new RuntimeException("SSH command failed: " + command, e);
        }
    }
    // In RgwService.java
    public String getBucketId(String bucketName) {
        String command = String.format(
                "sudo cephadm shell -- radosgw-admin bucket stats --bucket=%s --format=json",
                bucketName
        );
        String output = executeCommand(command);

        try {
            JsonNode json = new ObjectMapper().readTree(output);
            return json.get("id").asText();   // this is the real RGW bucket ID
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse bucket ID", e);
        }
    }
    public double getBucketSizeGB(String bucketName) {
        String command = String.format(
                "sudo cephadm shell -- radosgw-admin bucket stats --bucket=%s --format=json",
                bucketName
        );
        String output = executeCommand(command);

        try {
            JsonNode json = new ObjectMapper().readTree(output);
            // actual used bytes is inside usage.rgw.main.size_actual
            long bytes = json.path("usage")
                    .path("rgw.main")
                    .path("size_actual")
                    .asLong(0);
            return bytes / 1024.0 / 1024.0 / 1024.0;
        } catch (Exception e) {
            log.warn("Could not parse bucket stats for {}: {}", bucketName, e.getMessage());
            return 0.0;
        }
    }
}