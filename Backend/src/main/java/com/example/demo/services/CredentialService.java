package com.example.demo.services;

import com.example.demo.entities.AccessCredential;
import com.example.demo.entities.Project;
import com.example.demo.repositories.AccesCredentialRepository;
import com.example.demo.repositories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CredentialService {

    private final ProjectRepository projectRepository;
    private final AccesCredentialRepository credentialRepository;
    private final RgwService rgwService;

    public AccessCredential createCredential(Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        String uid = "proj-" + projectId + "-" + UUID.randomUUID();

        Map<String, String> keys = rgwService.createRgwUser(uid);

        AccessCredential credential = new AccessCredential();
        credential.setAccessKey(keys.get("accessKey"));
        credential.setSecretKey(keys.get("secretKey"));
        credential.setRgwUid(uid);
        credential.setProject(project);
        credential.setActive(true);

        return credentialRepository.save(credential);
    }
}
