package com.example.demo.services;

import com.jcraft.jsch.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class EsxiSshService {

    private static final String HOST = "192.168.100.100"; // ESXi IP
    private static final int PORT = 22;
    private static final String USER = "root";
    private static final String PASSWORD = "rootroot123?"; // or use key if you prefer

    public CommandResult execute(String command) {
        Session session = null;
        ChannelExec channel = null;

        try {
            JSch jsch = new JSch();

            session = jsch.getSession(USER, HOST, PORT);
            session.setPassword(PASSWORD);

            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(10000);

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            channel.setOutputStream(stdout);
            channel.setErrStream(stderr);

            channel.connect(10000);

            while (!channel.isClosed()) {
                Thread.sleep(200);
            }

            int exitCode = channel.getExitStatus();

            return new CommandResult(
                    exitCode,
                    stdout.toString(),
                    stderr.toString()
            );

        } catch (Exception e) {
            throw new RuntimeException("ESXi SSH command failed: " + command, e);
        } finally {
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
    }

    public String executeOrThrow(String command) {
        CommandResult result = execute(command);

        if (result.getExitCode() != 0) {
            throw new RuntimeException(
                    "ESXi command failed\n" +
                            "CMD: " + command + "\n" +
                            "STDOUT:\n" + result.getStdout() + "\n" +
                            "STDERR:\n" + result.getStderr()
            );
        }

        return result.getStdout();
    }
}