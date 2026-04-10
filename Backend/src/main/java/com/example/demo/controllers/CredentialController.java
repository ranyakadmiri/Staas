package com.example.demo.controllers;

import com.example.demo.entities.AccessCredential;
import com.example.demo.services.CredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CredentialController {
    private final CredentialService credentialService;

    @PostMapping("/projects/{projectId}/credentials")
    public ResponseEntity<AccessCredential> createCredential(
            @PathVariable Long projectId) {

        return ResponseEntity.ok(
                credentialService.createCredential(projectId)
        );
    }
    @GetMapping("/my-credentials")
    public List<AccessCredential> getMyCredentials(
            Principal principal){

        return credentialService.getUserCredentials(
                principal.getName()
        );
    }
}
