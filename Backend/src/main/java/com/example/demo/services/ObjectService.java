package com.example.demo.services;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.example.demo.entities.AccessCredential;
import com.example.demo.repositories.AccesCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RequiredArgsConstructor
@Service
public class ObjectService {

    private final AccesCredentialRepository credentialRepository;

    public List<Map<String,Object>> listObjects(Long projectId, String bucketName) {

        AccessCredential credential =
                credentialRepository.findByProjectId(projectId);

        String accessKey = credential.getAccessKey();
        String secretKey = credential.getSecretKey();

        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                "http://192.168.56.11:80",
                                "us-east-1"
                        )
                )
                .withPathStyleAccessEnabled(true)
                .withCredentials(
                        new AWSStaticCredentialsProvider(
                                new BasicAWSCredentials(accessKey, secretKey)
                        )
                )
                .build();

        ListObjectsV2Result result = s3.listObjectsV2(bucketName);

        List<Map<String,Object>> objects = new ArrayList<>();

        for (S3ObjectSummary obj : result.getObjectSummaries()) {

            Map<String,Object> file = new HashMap<>();

            file.put("name", obj.getKey());
            file.put("size", obj.getSize());
            file.put("lastModified", obj.getLastModified());

            objects.add(file);
        }

        return objects;
    }
}