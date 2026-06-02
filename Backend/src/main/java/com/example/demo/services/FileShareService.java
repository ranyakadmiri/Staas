package com.example.demo.services;

import com.example.demo.config.StorageProperties;
import com.example.demo.dto.FileEntryDTO;
import com.example.demo.dto.FileShareResponse;
import com.example.demo.dto.MountInfoResponse;
import com.example.demo.entities.FileShare;
import com.example.demo.repositories.FileShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileShareService {

    private final FileShareRepository repository;
    private final CephFsService cephFsService;
    private final NfsExportService nfsExportService;
    private final StorageProperties properties;

    public FileShareResponse createShare(Long projectId, String name) {

        validateShareName(name);

        if (repository.existsByProjectIdAndName(projectId, name)) {
            throw new IllegalArgumentException("Share already exists in this project");
        }

        String shareKey = buildShareKey(projectId, name);
        String realPath = cephFsService.buildRealPath(shareKey);
        String pseudoPath = "/" + shareKey;

        FileShare share = FileShare.builder()
                .projectId(projectId)
                .name(name)
                .shareKey(shareKey)
                .realPath(realPath)
                .pseudoPath(pseudoPath)
                .serverIp(properties.getNfs().getServerIp())
                .exportId(nfsExportService.nextExportId())
                .status("CREATING")
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(share);

        try {

            System.out.println("🔹 STEP 1: Creating directory " + shareKey);
            cephFsService.createDirectory(shareKey);

            System.out.println("🔹 STEP 2: Creating NFS export");
            nfsExportService.createExport(share);

            // 🔥 VERIFY EXPORT REALLY WRITTEN
            System.out.println("🔹 STEP 3: Verifying export");

            String check = cephFsService.getSshService().executeOrThrow(
                    "grep -w '" + shareKey + "' /etc/ganesha/ganesha.conf || echo NOT_FOUND"
            );
            System.out.println("EXPORT CHECK: " + check);

            if (check.contains("NOT_FOUND")) {
                throw new RuntimeException("Export was not written to ganesha.conf");
            }

            System.out.println("🔹 STEP 4: Share ready — NFS export actif");

            System.out.println("🔹 STEP 5: SUCCESS");

            share.setStatus("READY");
            repository.save(share);

            return toResponse(share);

        } catch (Exception e) {

            System.err.println("❌ ERROR DURING SHARE CREATION:");
            e.printStackTrace();

            share.setStatus("ERROR");
            repository.save(share);

            throw new RuntimeException("Failed to create file share: " + e.getMessage(), e);
        }
    }

    public List<FileShareResponse> listShares(Long projectId) {
        return repository.findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

   public FileShareResponse getShare(Long projectId, String name) {
        FileShare share = repository.findByProjectIdAndName(projectId, name)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        return toResponse(share);
    }


    /*public MountInfoResponse getMountInfo(Long projectId, String name) {
        FileShare share = repository.findByProjectIdAndName(projectId, name)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        return MountInfoResponse.builder()
                .server(share.getServerIp())
                .path(share.getPseudoPath())
                .mountTarget(share.getServerIp() + ":" + share.getPseudoPath())
                .protocol("NFS")
                .esxiVersion("NFS 4")
                .build();
    }*/
    public FileShareResponse getMountInfo(Long projectId, String name) {
        FileShare share = repository.findByProjectIdAndName(projectId, name)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));
        return toResponse(share);  // réutilise toResponse() directement
    }

    public List<FileEntryDTO> browseShare(Long projectId, String name) {
        FileShare share = repository.findByProjectIdAndName(projectId, name)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        return cephFsService.listDirectory(share.getShareKey());
    }

    public String getShareSize(Long projectId, String name) {
        FileShare share = repository.findByProjectIdAndName(projectId, name)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        return cephFsService.getDirectorySize(share.getShareKey());
    }

    public void uploadFile(Long projectId, String name, MultipartFile file) {
        FileShare share = repository.findByProjectIdAndName(projectId, name)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        try (InputStream in = file.getInputStream()) {
            cephFsService.uploadFile(share.getShareKey(), file.getOriginalFilename(), in);
        } catch (Exception e) {
            throw new RuntimeException("Upload failed", e);
        }
    }

    public void deleteShare(Long projectId, String name) {
        FileShare share = repository.findByProjectIdAndName(projectId, name)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        share.setStatus("DELETING");
        repository.save(share);

        try {
            nfsExportService.deleteExport(share);
            cephFsService.deleteDirectory(share.getShareKey());
            repository.delete(share);
        } catch (Exception e) {
            share.setStatus("ERROR");
            repository.save(share);
            throw new RuntimeException("Failed to delete share", e);
        }
    }

    private String buildShareKey(Long projectId, String name) {
        return "proj-" + projectId + "-" + name;
    }

    private void validateShareName(String name) {
        if (name == null || !name.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Invalid share name");
        }
    }

   /* private FileShareResponse toResponse(FileShare share) {
        return FileShareResponse.builder()
                .id(share.getId())
                .projectId(share.getProjectId())
                .name(share.getName())
                .shareKey(share.getShareKey())
                .pseudoPath(share.getPseudoPath())
                .realPath(share.getRealPath())
                .serverIp(share.getServerIp())
                .exportId(share.getExportId())
                .status(share.getStatus())
                .mountTarget(share.getServerIp() + ":" + share.getPseudoPath())
                .build();
    }*/
   // Remplacer toResponse() par ceci :
   private FileShareResponse toResponse(FileShare share) {
       String server = share.getServerIp();
       String exportPath = share.getPseudoPath();

       return FileShareResponse.builder()
               .id(share.getId())
               .projectId(share.getProjectId())
               .name(share.getName())
               .status(share.getStatus())
               .mountInfo(FileShareResponse.MountInfo.builder()
                       .server(server)
                       .exportPath(exportPath)
                       .nfsVersion("NFSv4")
                       .linuxCommand(
                               "sudo apt install nfs-common && " +
                                       "sudo mount -t nfs -o vers=4 " + server + ":" + exportPath + " /mnt/data"
                       )
                       .windowsCommand(
                               "net use Z: \\\\" + server + "\\" + exportPath.replace("/", "\\")
                       )
                       .macosCommand(
                               "sudo mount -t nfs " + server + ":" + exportPath + " /Volumes/data"
                       )
                       .build())
               .build();
   }
}