package com.example.demo.controllers;

import com.example.demo.repositories.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/agents")
@RequiredArgsConstructor
public class ProjectAgentController {

    private final AgentRepository agentRepository;

    /**
     * GET /api/projects/9/agents
     * Lists all registered agents for a project.
     * Use this to get the agentId before calling attach/detach.
     */
    @GetMapping
    public ResponseEntity<?> listAgents(@PathVariable Long projectId) {
        return ResponseEntity.ok(agentRepository.findByProjectId(projectId));
    }
}