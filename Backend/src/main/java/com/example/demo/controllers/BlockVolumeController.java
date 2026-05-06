package com.example.demo.controllers;

import com.example.demo.dto.CreateBlockVolumeRequest;
import com.example.demo.services.BlockVolumeService;
import com.example.demo.services.ProvisionEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/block-volumes")
@RequiredArgsConstructor
public class BlockVolumeController {

    private final BlockVolumeService blockVolumeService;
    private final ProvisionEventService eventService;

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long projectId,
                                    @RequestBody CreateBlockVolumeRequest request) {
        return ResponseEntity.ok(blockVolumeService.createVolume(projectId, request));
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(blockVolumeService.listProjectVolumes(projectId));
    }

    @GetMapping("/{name}")
    public ResponseEntity<?> get(@PathVariable Long projectId,
                                 @PathVariable String name) {
        return ResponseEntity.ok(blockVolumeService.getVolume(projectId, name));
    }

    @GetMapping("/{name}/events")
    public ResponseEntity<?> events(@PathVariable Long projectId,
                                    @PathVariable String name) {
        var volume = blockVolumeService.getVolume(projectId, name);
        return ResponseEntity.ok(eventService.getEvents(volume.getId()));
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<?> delete(@PathVariable Long projectId,
                                    @PathVariable String name) {
        blockVolumeService.deleteVolume(projectId, name);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }
}