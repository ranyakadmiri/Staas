package com.example.demo.controllers;

import com.example.demo.dto.CreateFileShareRequest;
import com.example.demo.services.FileShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/shares")
@RequiredArgsConstructor
public class FileShareController {

    private final FileShareService fileShareService;

    @PostMapping
    public ResponseEntity<?> createShare(@PathVariable Long projectId,
                                         @RequestBody CreateFileShareRequest request) {
        return ResponseEntity.ok(fileShareService.createShare(projectId, request.getName()));
    }

    @GetMapping
    public ResponseEntity<?> listShares(@PathVariable Long projectId) {
        return ResponseEntity.ok(fileShareService.listShares(projectId));
    }

    @GetMapping("/{name}")
    public ResponseEntity<?> getShare(@PathVariable Long projectId,
                                      @PathVariable String name) {
        return ResponseEntity.ok(fileShareService.getShare(projectId, name));
    }

    @GetMapping("/{name}/mount")
    public ResponseEntity<?> mountInfo(@PathVariable Long projectId,
                                       @PathVariable String name) {
        return ResponseEntity.ok(fileShareService.getMountInfo(projectId, name));
    }

    @GetMapping("/{name}/browse")
    public ResponseEntity<?> browse(@PathVariable Long projectId,
                                    @PathVariable String name) {
        return ResponseEntity.ok(fileShareService.browseShare(projectId, name));
    }

    @GetMapping("/{name}/size")
    public ResponseEntity<?> size(@PathVariable Long projectId,
                                  @PathVariable String name) {
        return ResponseEntity.ok(Map.of(
                "name", name,
                "size", fileShareService.getShareSize(projectId, name)
        ));
    }

    @PostMapping("/{name}/upload")
    public ResponseEntity<?> upload(@PathVariable Long projectId,
                                    @PathVariable String name,
                                    @RequestParam("file") MultipartFile file) {
        fileShareService.uploadFile(projectId, name, file);
        return ResponseEntity.ok(Map.of(
                "message", "Uploaded",
                "file", file.getOriginalFilename()
        ));
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<?> delete(@PathVariable Long projectId,
                                    @PathVariable String name) {
        fileShareService.deleteShare(projectId, name);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }
}
