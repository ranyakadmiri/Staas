package com.example.demo.services;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
@Service
public class BlockStorageService {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 2222;
    private static final String USER = "vagrant";
    private static final String PRIVATE_KEY =
            "C:/vagrant/staas-vagrant/.vagrant/machines/ceph-node-1/virtualbox/private_key";

    private static final String POOL = "rbd";

    private String executeCommand(String command) {

        try {
            JSch jsch = new JSch();
            jsch.addIdentity(PRIVATE_KEY);

            Session session = jsch.getSession(USER, HOST, PORT);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("sudo cephadm shell -- " + command);

            InputStream inputStream = channel.getInputStream();
            channel.connect();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            channel.disconnect();
            session.disconnect();

            return output.toString();

        } catch (Exception e) {
            throw new RuntimeException("Block command failed", e);
        }
    }

    // ✅ CREATE VOLUME
    public void createVolume(String name, int sizeGB) {

        String cmd = "rbd create " + name +
                " --size " + (sizeGB * 1024) +
                " --pool " + POOL;

        executeCommand(cmd);
    }

    // ✅ LIST VOLUMES
    public List<String> listVolumes() {

        String output = executeCommand("rbd ls --pool " + POOL);

        return List.of(output.split("\n"));
    }

    // ✅ DELETE VOLUME
    public void deleteVolume(String name) {

        String cmd = "rbd rm " + name + " --pool " + POOL;

        executeCommand(cmd);
    }

    // ✅ VOLUME INFO
    public String getVolumeInfo(String name) {

        return executeCommand(
                "rbd info " + name + " --pool " + POOL
        );
    }
    public boolean volumeExists(String name) {
        return listVolumes().contains(name);
    }
    public String getPool() {
        return POOL;
    }
}