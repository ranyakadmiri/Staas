package com.example.demo.controllers;

import com.example.demo.dto.UpdateQuotaRequest;
import com.example.demo.entities.BucketQuota;
import com.example.demo.services.BucketQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/buckets/quota")
@RequiredArgsConstructor
public class BucketQuotaController {
    private final BucketQuotaService quotaService;

    @GetMapping("/{bucketId}")
    public ResponseEntity<BucketQuota> getQuota(@PathVariable Long bucketId) {

        return ResponseEntity.ok(
                quotaService.getQuota(bucketId)
        );
    }

    @PutMapping("/{bucketId}")
    public ResponseEntity<BucketQuota> updateQuota(
            @PathVariable Long bucketId,
            @RequestBody UpdateQuotaRequest request) {

        return ResponseEntity.ok(
                quotaService.updateQuota(
                        bucketId,
                        request.getMaxSizeGB(),
                        request.getMaxObjects()
                )
        );
    }

    @PostMapping("/{bucketId}")
    public ResponseEntity<BucketQuota> createQuota(
            @PathVariable Long bucketId,
            @RequestBody UpdateQuotaRequest request) {

        return ResponseEntity.ok(
                quotaService.createQuota(
                        bucketId,
                        request.getMaxSizeGB(),
                        request.getMaxObjects()
                )
        );
    }
}
