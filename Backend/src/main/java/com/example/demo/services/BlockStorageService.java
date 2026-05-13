package com.example.demo.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockStorageService {

    private final CephSshService sshService;

    private static final String POOL = "rbd";

    public void createVolume(String name, int sizeGB) {
        String cmd = String.format(
                "rbd create %s --size %d --pool %s",
                name,
                sizeGB * 1024,
                POOL
        );
        sshService.executeCeph(cmd);
    }

    public List<String> listVolumes() {
        String output = sshService.executeCeph("rbd ls --pool " + POOL);
        return Arrays.stream(output.split("\n"))
                .filter(s -> !s.isBlank())
                .toList();
    }

    public void deleteVolume(String name) {
        String cmd = String.format("rbd rm %s --pool %s", name, POOL);
        sshService.executeCeph(cmd);
    }

    public String getVolumeInfo(String name) {
        return sshService.executeCeph("rbd info " + name + " --pool " + POOL);
    }

    public boolean volumeExists(String name) {
        try {
            sshService.executeCeph("rbd info " + name + " --pool " + POOL);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getPool() {
        return POOL;
    }
    public void expandVolume(String name, int newSizeGB) {

        String cmd = String.format(
                "rbd resize %s --size %d --pool %s",
                name,
                newSizeGB * 1024,
                POOL
        );

        sshService.executeCeph(cmd);
    }

}