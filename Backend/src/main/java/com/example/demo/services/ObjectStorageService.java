package com.example.demo.services;

import com.example.demo.dto.BucketStatsDTO;
import com.example.demo.entities.AccessCredential;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ObjectStorageService {
    private static final String ENDPOINT = "http://192.168.56.11:80";

    public S3Client buildClient(AccessCredential credential) {

        return S3Client.builder()
                .endpointOverride(URI.create(ENDPOINT))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        credential.getAccessKey(),
                                        credential.getSecretKey()
                                )
                        )
                )
                .region(Region.US_EAST_1)
                .forcePathStyle(true) // VERY IMPORTANT for Ceph
                .build();
    }

    public void createBucket(String bucketName, AccessCredential credential) {

        S3Client s3 = buildClient(credential);

        s3.createBucket(CreateBucketRequest.builder()
                .bucket(bucketName)
                .build());
    }

    public List<String> listBuckets(AccessCredential credential) {

        S3Client s3 = buildClient(credential);

        return s3.listBuckets()
                .buckets()
                .stream()
                .map(Bucket::name)
                .toList();
    }
    public void deleteBucket(String bucketName, AccessCredential credential) {

        S3Client s3 = buildClient(credential);

        s3.deleteBucket(DeleteBucketRequest.builder()
                .bucket(bucketName)
                .build());
    }
    public void uploadObject(
            String bucketName,
            String key,
            MultipartFile file,
            AccessCredential credential) throws IOException {

        S3Client s3 = buildClient(credential);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3.putObject(
                request,
                RequestBody.fromInputStream(
                        file.getInputStream(),
                        file.getSize()
                )
        );
    }
    public BucketStatsDTO getBucketStats(
            String bucketName,
            AccessCredential credential) {

        S3Client s3 = buildClient(credential);

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        ListObjectsV2Response response = s3.listObjectsV2(request);

        long totalSize = 0;
        int count = 0;

        long largestSize = 0;
        String largestObject = null;

        Instant latestDate = null;
        String latestObject = null;

        for (S3Object obj : response.contents()) {

            count++;
            totalSize += obj.size();

            if(obj.size() > largestSize){
                largestSize = obj.size();
                largestObject = obj.key();
            }

            if(latestDate == null || obj.lastModified().isAfter(latestDate)){
                latestDate = obj.lastModified();
                latestObject = obj.key();
            }
        }

        HeadBucketResponse bucketInfo =
                s3.headBucket(HeadBucketRequest.builder()
                        .bucket(bucketName)
                        .build());

        BucketStatsDTO stats = new BucketStatsDTO();

        stats.setBucketName(bucketName);
        stats.setObjectCount(count);
        stats.setTotalSizeBytes(totalSize);
        stats.setTotalSizeMB(totalSize / 1024.0 / 1024.0);
        stats.setTotalSizeGB(totalSize / 1024.0 / 1024.0 / 1024.0);

        stats.setLargestObject(largestObject);
        stats.setLargestObjectSize(largestSize);

        stats.setLastObject(latestObject);

        return stats;
    }
}
