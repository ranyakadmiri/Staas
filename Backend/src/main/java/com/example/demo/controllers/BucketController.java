package com.example.demo.controllers;

import com.example.demo.entities.AccessCredential;
import com.example.demo.repositories.AccesCredentialRepository;
import com.example.demo.services.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@RestController
@RequestMapping("/api/buckets")
@RequiredArgsConstructor
public class BucketController {
    private final ObjectStorageService objectStorageService;
    private final AccesCredentialRepository credentialRepository;

    @PostMapping("create/{projectId}")
    public ResponseEntity<?> createBucket(
            @PathVariable Long projectId,
            @RequestParam String bucketName) {

      AccessCredential credential =
                credentialRepository.findByProjectId(projectId);

        objectStorageService.createBucket(bucketName, credential);

        return ResponseEntity.ok("Bucket created");
    }

    @GetMapping("list/{projectId}")
    public ResponseEntity<?> listBuckets(@PathVariable Long projectId) {

        AccessCredential credential =
                credentialRepository.findByProjectId(projectId);

        return ResponseEntity.ok(
                objectStorageService.listBuckets(credential)
        );
    }
    @DeleteMapping("delete/{projectId}")
    public ResponseEntity<?> deleteBucket(
            @PathVariable Long projectId,
            @RequestParam String bucketName) {

        AccessCredential credential =
                credentialRepository.findByProjectId(projectId);

        objectStorageService.deleteBucket(bucketName, credential);

        return ResponseEntity.ok("Bucket deleted");
    }
    @PostMapping("/{projectId}/upload")
    public ResponseEntity<?> uploadObject(
            @PathVariable Long projectId,
            @RequestParam String bucketName,
            @RequestParam MultipartFile file) throws IOException {

      AccessCredential credential =
                credentialRepository.findByProjectId(projectId);

        objectStorageService.uploadObject(
                bucketName,
                file.getOriginalFilename(),
                file,
                credential
        );

        return ResponseEntity.ok("File uploaded");
    }
    @GetMapping("/{projectId}/stats")
    public ResponseEntity<?> getBucketStats(
            @PathVariable Long projectId,
            @RequestParam String bucketName) {

        AccessCredential credential =
                credentialRepository.findByProjectId(projectId);

        return ResponseEntity.ok(
                objectStorageService.getBucketStats(bucketName, credential)
        );
    }
}
