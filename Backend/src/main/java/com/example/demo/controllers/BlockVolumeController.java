package com.example.demo.controllers;

import com.example.demo.dto.BlockVolumeResponse;
import com.example.demo.dto.CreateBlockVolumeRequest;
import com.example.demo.repositories.BlockVolumeRepository;
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
    private final BlockVolumeRepository repository; // add this

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
        // BlockVolumeResponse has getId() — it's in the refactored DTO
        BlockVolumeResponse volume = blockVolumeService.getVolume(projectId, name);
        return ResponseEntity.ok(eventService.getEvents(volume.getId()));
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<?> delete(@PathVariable Long projectId,
                                    @PathVariable String name) {
        blockVolumeService.deleteVolume(projectId, name);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    @PostMapping("/{name}/extents")
    public ResponseEntity<BlockVolumeResponse> appendExtent(
            @PathVariable Long projectId,
            @PathVariable String name,
            @RequestBody CreateBlockVolumeRequest dto) {
        return ResponseEntity.ok(
                blockVolumeService.appendDiskToVolume(projectId, name, dto)
        );
    }
}