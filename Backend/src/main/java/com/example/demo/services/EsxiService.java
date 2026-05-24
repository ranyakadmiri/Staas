package com.example.demo.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EsxiService {


    private static final String GOVC = "C:\\govc\\govc.exe";
    private static final String ESXI_HOST = "192.168.100.100";
    private static final String ESXI_USER = "root";
    private static final String ESXI_PASS = "rootroot123%3F";

    private final EsxiSshService ssh;

    private String run(String... args) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(GOVC);

            for (String arg : args) {
                cmd.add(arg);
            }

            cmd.add("-u=https://" + ESXI_USER + ":" + ESXI_PASS + "@" + ESXI_HOST);
            cmd.add("-k");

            log.info("[govc] {}", String.join(" ", cmd).replace(ESXI_PASS, "****"));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);

            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            int exit = p.waitFor();

            log.info("[govc] exit={} output={}", exit, out);

            if (exit != 0) {
                throw new RuntimeException("govc failed: " + out);
            }

            return out;

        } catch (Exception e) {
            throw new RuntimeException("govc execution error", e);
        }
    }
    public List<String> listDisks() {

        String output = run("host.storage.info");

        return Arrays.stream(output.split("\n"))
                .filter(line -> line.contains("/vmfs/devices/disks/"))
                .map(line -> {
                    String path = line.split("\\s+")[0].trim();
                    return path.substring(path.lastIndexOf("/") + 1);
                })
                .toList();
    }


    public void rescan() {
        run("host.storage.info", "-rescan");
    }

   /* public String detectNewDisk(Set<String> before) {

        for (int i = 0; i < 15; i++) {

            log.info("[ESXi] Rescan attempt {}", i + 1);

            rescan();
            sleep(2000);

            List<String> after = listDisks();

            for (String disk : after) {
                if (!before.contains(disk)) {
                    log.info("[ESXi] NEW disk detected: {}", disk);
                    return disk;
                }
            }
        }

        throw new RuntimeException("No new disk detected");
    }*/
   public String detectNewDisk(Set<String> before) {

       for (int i = 0; i < 15; i++) {

           log.info("[ESXi] Rescan attempt {}", i + 1);

           rescan();
           sleep(2000);

           List<String> after = listDisks();

           // ✅ CASE 1: new disk appeared
           if (after.size() > before.size()) {

               // return the one not in before
               for (String disk : after) {
                   if (!before.contains(disk)) {
                       log.info("[ESXi] NEW disk detected: {}", disk);
                       return disk;
                   }
               }
           }

           // ✅ CASE 2: disk already existed → fallback
           // pick LAST disk (most recently exposed)
           if (!after.isEmpty()) {
               String last = after.get(after.size() - 1);
               log.info("[ESXi] Using last disk as fallback: {}", last);
               return last;
           }
       }

       throw new RuntimeException("No disk detected");
   }
    public String detectDiskByIqn(String targetIqn) {

        for (int i = 0; i < 10; i++) {

            log.info("[ESXi] Searching disk for IQN {}", targetIqn);

            rescan();
            sleep(3000);

            String targets = run("iscsi.adapter.target.list");

            if (!targets.contains(targetIqn)) {
                continue;
            }

            String devices = run("iscsi.adapter.device.list");

            String disk = Arrays.stream(devices.split("\n"))
                    .filter(line -> line.contains(targetIqn))
                    .map(line -> line.split("\\s+")[0]) // naa.xxx
                    .findFirst()
                    .orElse(null);

            if (disk != null) {
                log.info("[ESXi] Found disk {} for IQN {}", disk, targetIqn);
                return disk;
            }
        }

        throw new RuntimeException("Disk not found for IQN " + targetIqn);
    }
    public void createDatastore(String name, String disk) {

        String base = "/vmfs/devices/disks/" + disk;

        // 1️⃣ get size
        String output = ssh.executeOrThrow(
                "partedUtil getptbl " + base
        );

        long total = Long.parseLong(output.split("\n")[1].split(" ")[3]);
        long end = total - 34;

        // 2️⃣ partition
        ssh.executeOrThrow(String.format(
                "partedUtil setptbl %s gpt \"1 2048 %d AA31E02A400F11DB9590000C2911D1B8 0\"",
                base, end
        ));

        // 3️⃣ VMFS
        ssh.executeOrThrow(String.format(
                "vmkfstools -C vmfs6 -S %s %s:1",
                name, base
        ));
    }
    public void deleteDatastore(String datastoreName) {
        try {
            run("datastore.remove", "-ds=" + datastoreName);
        } catch (Exception e) {
            log.warn("[ESXi] Could not delete datastore {}: {}", datastoreName, e.getMessage());
        }
    }

    public String listDatastores() {
        return run("datastore.info", "*");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
    public void createNfsDatastore(String name, String serverIp, String remotePath) {
        try {
            List<String> cmd = List.of(
                    "C:\\govc\\govc.exe",
                    "host.esxcli",
                    "storage",
                    "nfs41",
                    "add",
                    "-hosts", serverIp,       // ← nfs41 uses -hosts (plural)
                    "-share", remotePath,
                    "-volume-name", name,
                    "-sec", "AUTH_SYS"        // ← matches SecType = sys in ganesha
            );

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().put("GOVC_URL", "https://" + ESXI_HOST + "/sdk");
            pb.environment().put("GOVC_USERNAME", ESXI_USER);
            pb.environment().put("GOVC_PASSWORD", ESXI_PASS);
            pb.environment().put("GOVC_INSECURE", "true");
            pb.environment().put("GOVC_DATACENTER", "ha-datacenter");
            pb.redirectErrorStream(true);

            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            int exit = p.waitFor();

            System.out.println("=== govc NFS41 output: " + out);
            System.out.println("=== govc exit code: " + exit);

            if (exit != 0) {
                throw new RuntimeException("govc failed: " + out);
            }

        } catch (Exception e) {
            throw new RuntimeException("govc execution error", e);
        }
    }
    public void appendDiskToDatastore(String datastoreName, String newDisk) {

        String base = "/vmfs/devices/disks/" + newDisk;

        // Step 1: create GPT label on new disk
        ssh.executeOrThrow("partedUtil mklabel " + base + " gpt");

        // Step 2: get usable sectors
        String usable = ssh.executeOrThrow(
                "partedUtil getUsableSectors " + base).trim();
        long end = Long.parseLong(usable.split("\\s+")[1]);

        // Step 3: create VMFS partition
        ssh.executeOrThrow(String.format(
                "partedUtil setptbl %s gpt \"1 2048 %d AA31E02A400F11DB9590000C2911D1B8 0\"",
                base, end));

        // Step 4: refresh
        ssh.executeOrThrow("vmkfstools -V");
        sleep(3000);

        // Step 5: find head disk NAA id (cols[3] fix from earlier)
        String headDisk = findDatastoreExtent(datastoreName);

        // Step 6: span using raw device paths for BOTH arguments
        // -Z <new-partition> <head-raw-device-partition>
        // NOTE: volume name and UUID do NOT work — must use /vmfs/devices/disks/
        ssh.executeOrThrow(String.format(
                "echo 0 | vmkfstools -Z %s:1 /vmfs/devices/disks/%s:1",
                base,       // new disk  (first arg)
                headDisk    // head disk (second arg — raw device path)
        ));

        // Step 7: final refresh
        ssh.executeOrThrow("vmkfstools -V");
    }
    public String findDatastoreExtent(String datastoreName) {

        String out = ssh.executeOrThrow("esxcli storage vmfs extent list");

        for (String line : out.split("\n")) {

            // Skip header and separator lines
            if (line.trim().isEmpty() ||
                    line.trim().startsWith("-") ||
                    line.contains("Volume Name")) continue;

            if (line.contains(datastoreName)) {
                // Split on 2+ spaces to handle column alignment correctly
                // Columns: Volume Name | VMFS UUID | Extent Number | Device Name | Partition
                String[] cols = line.trim().split("\\s{2,}");

                if (cols.length < 4) {
                    throw new RuntimeException(
                            "Unexpected extent list format: " + line);
                }

                // Log all columns to verify alignment
                for (int i = 0; i < cols.length; i++) {
                    System.out.println("col[" + i + "] = " + cols[i]);
                }

                return cols[3].trim(); // Device Name = NAA id
            }
        }

        throw new RuntimeException(
                "Could not find datastore extent for " + datastoreName);
    }

}