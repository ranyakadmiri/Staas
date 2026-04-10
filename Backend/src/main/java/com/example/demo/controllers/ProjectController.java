package com.example.demo.controllers;

import com.example.demo.dto.CreateProjectRequest;
import com.example.demo.entities.Project;
import com.example.demo.services.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/createprojects")
    public ResponseEntity<Project> createProject(
            @RequestBody CreateProjectRequest request,
            Principal principal) {

        Project project = projectService.createProject(
                request,
                principal.getName()
        );

        return ResponseEntity.ok(project);
    }

    @GetMapping("/ListProjects")
    public ResponseEntity<List<Project>> getProjects(Principal principal) {
        return ResponseEntity.ok(
                projectService.getUserProjects(principal.getName())
        );
    }
}
