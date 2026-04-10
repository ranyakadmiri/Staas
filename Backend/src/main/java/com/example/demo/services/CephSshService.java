package com.example.demo.services;

import com.jcraft.jsch.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class CephSshService {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 2222;
    private static final String USER = "vagrant";
    private static final String PRIVATE_KEY =
            "C:/vagrant/staas-vagrant/.vagrant/machines/ceph-node-1/virtualbox/private_key";

    public CommandResult execute(String command) {
        Session session = null;
        ChannelExec channel = null;

        try {
            JSch jsch = new JSch();
            jsch.addIdentity(PRIVATE_KEY);

            session = jsch.getSession(USER, HOST, PORT);
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
            return new CommandResult(exitCode, stdout.toString(), stderr.toString());

        } catch (Exception e) {
            throw new RuntimeException("SSH command failed: " + command, e);
        } finally {
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
    }

    public String executeOrThrow(String command) {
        CommandResult result = execute(command);
        if (result.getExitCode() != 0) {
            throw new RuntimeException(
                    "Command failed.\nCMD: " + command +
                            "\nSTDOUT:\n" + result.getStdout() +
                            "\nSTDERR:\n" + result.getStderr()
            );
        }
        return result.getStdout();
    }

    public String executeCeph(String cephCommand) {
        String full = "sudo cephadm shell -- " + cephCommand;
        return executeOrThrow(full);
    }

    public String executeBash(String bashScript) {
        String escaped = bashScript.replace("'", "'\"'\"'");
        String full = "sudo bash -lc '" + escaped + "'";
        return executeOrThrow(full);
    }
}