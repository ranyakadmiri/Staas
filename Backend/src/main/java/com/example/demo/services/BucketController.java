package com.example.demo.services;

import com.example.demo.entities.AccessCredential;
import com.example.demo.repositories.AccesCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/buckets")
@RequiredArgsConstructor
public class BucketController {
    private final ObjectStorageService objectStorageService;
    private final AccesCredentialRepository credentialRepository;

    @PostMapping("/{projectId}")
    public ResponseEntity<?> createBucket(
            @PathVariable Long projectId,
            @RequestParam String bucketName) {

      AccessCredential credential =
                credentialRepository.findByProjectId(projectId);

        objectStorageService.createBucket(bucketName, credential);

        return ResponseEntity.ok("Bucket created");
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<?> listBuckets(@PathVariable Long projectId) {

        AccessCredential credential =
                credentialRepository.findByProjectId(projectId);

        return ResponseEntity.ok(
                objectStorageService.listBuckets(credential)
        );
    }
}
