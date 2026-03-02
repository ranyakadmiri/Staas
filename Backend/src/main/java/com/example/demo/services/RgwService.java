package com.example.demo.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
@Service
public class RgwService {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 2222;
    private static final String USER = "vagrant";
    private static final String PRIVATE_KEY = "C:/vagrant/staas-vagrant/.vagrant/machines/ceph-node-1/virtualbox/private_key";

    public Map<String, String> createRgwUser(String uid) {

        try {
            JSch jsch = new JSch();
            jsch.addIdentity(PRIVATE_KEY);

            Session session = jsch.getSession(USER, HOST, PORT);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            String command = "sudo cephadm shell -- radosgw-admin user create " +
                    "--uid=" + uid +
                    " --display-name=" + uid +
                    " --format=json";

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            InputStream inputStream = channel.getInputStream();
            channel.connect();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            channel.disconnect();
            session.disconnect();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(output.toString());

            String accessKey = json.get("keys").get(0).get("access_key").asText();
            String secretKey = json.get("keys").get(0).get("secret_key").asText();

            Map<String, String> result = new HashMap<>();
            result.put("accessKey", accessKey);
            result.put("secretKey", secretKey);

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create RGW user via SSH", e);
        }
    }
}