package com.example.demo.services;

import com.example.demo.dto.BucketStatsDTO;
import com.example.demo.entities.*;
import com.example.demo.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
public class UsageCollectorService {

    private final ProjectRepository projectRepository;
    private final BucketRepository bucketRepository;
    private final FileShareRepository nfsShareRepository;
    private final BlockVolumeRepository iscsiTargetRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final ObjectStorageService objectStorageService;
    private final FileStorageService fileStorageService;
    private final AccesCredentialRepository credentialRepository;
    private final RgwService rgwService;

    @Scheduled(cron = "0 0 0 * * *")
    public void collectDailyUsage() {
        String period = YearMonth.now().toString();
        List<Project> projects = projectRepository.findAll();

        for (Project project : projects) {
            collectObjectStorageUsage(project, period);
            collectBlockStorageUsage(project, period);
            collectFilesystemUsage(project, period);
        }
    }

    // ── OBJECT: one record per bucket + one project aggregate ────────────────

    private void collectObjectStorageUsage(Project project, String period) {
        try {
            AccessCredential credential = credentialRepository.findByProjectId(project.getId());
            if (credential == null) return;

            List<Bucket> buckets = bucketRepository.findByProjectId(project.getId());
            if (buckets.isEmpty()) return;

            double totalGB = 0;

            for (Bucket bucket : buckets) {
                try {
                    double bucketGB = rgwService.getBucketSizeGB(bucket.getName());
                    totalGB += bucketGB;
                    saveRecord(project, bucket, ResourceType.OBJECT, bucketGB, period);
                } catch (Exception e) {
                    log.warn("Could not get stats for bucket {}: {}", bucket.getName(), e.getMessage());
                }
            }

            // project-level aggregate (bucket = null)
            saveRecord(project, null, ResourceType.OBJECT, totalGB, period);

        } catch (Exception e) {
            log.error("Object storage collection failed for project {}", project.getId(), e);
        }
    }

    // ── BLOCK: one record per READY volume + one project aggregate ────────────

    private void collectBlockStorageUsage(Project project, String period) {
        try {
            List<BlockVolume> volumes = iscsiTargetRepository.findByProjectId(project.getId());
            if (volumes.isEmpty()) return;

            double totalGB = 0;

            for (BlockVolume volume : volumes) {
                if (volume.getStatus() != BlockVolumeStatus.READY) continue;

                double volumeGB = volume.getSizeGB();   // already stored in entity
                totalGB += volumeGB;
            }

            if (totalGB == 0) return;

            saveRecord(project, null, ResourceType.BLOCK, totalGB, period);

        } catch (Exception e) {
            log.error("Block storage collection failed for project {}", project.getId(), e);
        }
    }

    // ── FILESYSTEM: SSH du for each READY share + project aggregate ──────────

    private void collectFilesystemUsage(Project project, String period) {
        try {
            List<FileShare> shares = nfsShareRepository.findByProjectId(project.getId());
            if (shares.isEmpty()) return;

            double totalGB = 0;

            for (FileShare share : shares) {
                if (!"READY".equals(share.getStatus())) continue;

                try {
                    // shareKey = "proj-9-test" → maps to /mnt/cephfs/proj-9-test
                    double shareGB = fileStorageService.getDirectorySizeGB(share.getShareKey());
                    totalGB += shareGB;
                } catch (Exception e) {
                    log.warn("Could not get size for share {}: {}", share.getShareKey(), e.getMessage());
                }
            }

            if (totalGB == 0) return;

            saveRecord(project, null, ResourceType.FILE, totalGB, period);

        } catch (Exception e) {
            log.error("Filesystem collection failed for project {}", project.getId(), e);
        }
    }

    // ── single clean saveRecord ───────────────────────────────────────────────

    private void saveRecord(Project project, Bucket bucket, ResourceType type, double usedGB, String period) {
        UsageRecord record = new UsageRecord();
        record.setProject(project);
        record.setBucket(bucket);        // null for block/filesystem/aggregates
        record.setResourceType(type);
        record.setUsedGB(usedGB);
        record.setRecordedAt(LocalDateTime.now());
        record.setBillingPeriod(period);
        usageRecordRepository.save(record);
    }
}