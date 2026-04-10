package com.example.demo.services;

import com.example.demo.entities.Bucket;
import com.example.demo.entities.BucketQuota;
import com.example.demo.repositories.BucketQuotaRepository;
import com.example.demo.repositories.BucketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BucketQuotaService {


        private final BucketRepository bucketRepository;
        private final BucketQuotaRepository quotaRepository;


        public BucketQuota getQuota(Long bucketId) {

            return quotaRepository.findByBucketId(bucketId)
                    .orElseThrow(() -> new RuntimeException("Quota not found"));
        }


        public BucketQuota updateQuota(Long bucketId, Long maxSizeGB, Long maxObjects) {

            BucketQuota quota = quotaRepository.findByBucketId(bucketId)
                    .orElseThrow(() -> new RuntimeException("Quota not found"));

            quota.setMaxSizeGB(maxSizeGB);
            quota.setMaxObjects(maxObjects);

            return quotaRepository.save(quota);
        }


        public BucketQuota createQuota(Long bucketId, Long maxSizeGB, Long maxObjects) {

            Bucket bucket = bucketRepository.findById(bucketId)
                    .orElseThrow(() -> new RuntimeException("Bucket not found"));

            BucketQuota quota = new BucketQuota();
            quota.setBucket(bucket);
            quota.setMaxSizeGB(maxSizeGB);
            quota.setMaxObjects(maxObjects);
            quota.setUsedSizeGB(0L);

            return quotaRepository.save(quota);
        }
    }
