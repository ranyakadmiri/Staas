package com.example.demo.controllers;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the agent script for download.
 *
 * Place staas_agent.py in:
 *   src/main/resources/static/agent/staas_agent.py
 *
 * Client downloads with:
 *   curl http://yourserver:8080/api/agent/download -o staas_agent.py
 */
@RestController
@RequestMapping("/api/agent")
public class AgentDownloadController {

    @GetMapping("/download")
    public ResponseEntity<Resource> download() {
        Resource file = new ClassPathResource("static/agent/staas_agent.py");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"staas_agent.py\"")
                .body(file);
    }
}
