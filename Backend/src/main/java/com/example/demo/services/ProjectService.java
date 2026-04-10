package com.example.demo.services;

import com.example.demo.dto.CreateProjectRequest;
import com.example.demo.entities.Project;
import com.example.demo.entities.User;
import com.example.demo.repositories.ProjectRepository;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    private final CredentialService credentialService;

    public Project createProject(CreateProjectRequest request, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setResourceType(request.getResourceType());
        project.setRegion(request.getRegion());
        project.setMaxBuckets(request.getMaxBuckets());
        project.setMaxStorageGB(request.getMaxStorageGB());
        project.setOwner(user);


        Project savedProject = projectRepository.save(project);

        // 🔑 Generate credentials automatically
        credentialService.createCredential(savedProject.getId());

        return savedProject;
    }

    public List<Project> getUserProjects(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Query projects directly by owner
        return projectRepository.findByOwner(user);
    }
}
