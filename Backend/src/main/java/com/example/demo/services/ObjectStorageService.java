package com.example.demo.services;

import com.example.demo.entities.AccessCredential;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.net.URI;
import java.util.List;

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
}
