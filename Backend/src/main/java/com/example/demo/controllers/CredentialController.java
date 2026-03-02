package com.example.demo.controllers;

import com.example.demo.entities.AccessCredential;
import com.example.demo.services.CredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/credentials")
@RequiredArgsConstructor
public class CredentialController {
    private final CredentialService credentialService;

    @PostMapping
    public ResponseEntity<AccessCredential> createCredential(
            @PathVariable Long projectId) {

        return ResponseEntity.ok(
                credentialService.createCredential(projectId)
        );
    }
}
