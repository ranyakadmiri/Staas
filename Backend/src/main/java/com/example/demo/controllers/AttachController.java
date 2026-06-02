package com.example.demo.controllers;

import com.example.demo.dto.AttachRequest;
import com.example.demo.services.AttachService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/block-volumes/{volumeName}")
@RequiredArgsConstructor
public class AttachController {

    private final AttachService attachService;

    /**
     * POST /api/projects/9/block-volumes/my-volume/attach
     * { "agentId": 1 }
     */
    @PostMapping("/attach")
    public ResponseEntity<?> attach(
            @PathVariable Long projectId,
            @PathVariable String volumeName,
            @RequestBody AttachRequest request) {

        attachService.attach(projectId, volumeName, request.getAgentId());

        return ResponseEntity.ok(Map.of(
                "message", "Attach queued. Disk will appear on the VM within 10 seconds.",
                "agentId", request.getAgentId(),
                "volume",  volumeName
        ));
    }

    /**
     * POST /api/projects/9/block-volumes/my-volume/detach
     * { "agentId": 1 }
     */
    @PostMapping("/detach")
    public ResponseEntity<?> detach(
            @PathVariable Long projectId,
            @PathVariable String volumeName,
            @RequestBody AttachRequest request) {

        attachService.detach(projectId, volumeName, request.getAgentId());

        return ResponseEntity.ok(Map.of(
                "message", "Detach queued. Disk will be removed from the VM within 10 seconds.",
                "agentId", request.getAgentId(),
                "volume",  volumeName
        ));
    }
}
