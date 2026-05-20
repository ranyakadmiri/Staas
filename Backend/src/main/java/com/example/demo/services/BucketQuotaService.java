package com.example.demo.services;

import com.example.demo.entities.AccessCredential;
import com.example.demo.entities.Bucket;
import com.example.demo.entities.BucketQuota;
import com.example.demo.repositories.AccesCredentialRepository;
import com.example.demo.repositories.BucketQuotaRepository;
import com.example.demo.repositories.BucketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class BucketQuotaService {
    private final AccesCredentialRepository credentialRepository;
    private final BucketRepository bucketRepository;
    private final BucketQuotaRepository quotaRepository;
    private final RgwService rgwService;                     // ADD THIS

    public BucketQuota getQuota(Long bucketId) {
        return quotaRepository.findByBucketId(bucketId)
                .orElseThrow(() -> new RuntimeException("Quota not found"));
    }

    public BucketQuota createQuota(Long bucketId, Long maxSizeGB, Long maxObjects) {
        Bucket bucket = bucketRepository.findById(bucketId)
                .orElseThrow(() -> new RuntimeException("Bucket not found"));

        BucketQuota quota = new BucketQuota();
        quota.setBucket(bucket);
        quota.setMaxSizeGB(maxSizeGB);
        quota.setMaxObjects(maxObjects);
        quota.setUsedSizeGB(0L);

        BucketQuota saved = quotaRepository.save(quota);

        // ── sync to Ceph ──────────────────────────────────────────────────────
        Long projectId = bucket.getProject().getId();
        AccessCredential credential =
                credentialRepository.findByProjectId(projectId);// adjust to your model
        String uid = credential.getRgwUid();
        long maxSizeBytes = maxSizeGB * 1024L * 1024L * 1024L;
        rgwService.applyBucketQuota(uid, bucket.getName(), maxSizeBytes, maxObjects);

        return saved;
    }

    public BucketQuota updateQuota(Long bucketId, Long maxSizeGB, Long maxObjects) {
        BucketQuota quota = quotaRepository.findByBucketId(bucketId)
                .orElseThrow(() -> new RuntimeException("Quota not found"));

        quota.setMaxSizeGB(maxSizeGB);
        quota.setMaxObjects(maxObjects);

        BucketQuota saved = quotaRepository.save(quota);

        // ── sync to Ceph ──────────────────────────────────────────────────────
        Bucket bucket = quota.getBucket();
        Long projectId = bucket.getProject().getId();
        AccessCredential credential =
                credentialRepository.findByProjectId(projectId);// adjust to your model
        String uid = credential.getRgwUid();// adjust to your model
        long maxSizeBytes = maxSizeGB * 1024L * 1024L * 1024L;
        rgwService.applyBucketQuota(uid, bucket.getName(), maxSizeBytes, maxObjects);

        return saved;
    }
}