package com.example.demo.controllers;

import com.example.demo.services.FileStorageService;
import com.example.demo.services.FileStorageService.FileEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileService;

    @GetMapping("/list")
    public ResponseEntity<?> list() {
        try {
            return ResponseEntity.ok(fileService.listDirectories());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        try {
            fileService.createDirectory(body.get("name"));
            return ResponseEntity.ok(Map.of("message", "Directory created", "name", body.get("name")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/browse")
    public ResponseEntity<?> browse(@RequestParam String name) {
        try {
            List<FileEntry> entries = fileService.listDirectory(name);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/size")
    public ResponseEntity<?> size(@RequestParam String name) {
        try {
            return ResponseEntity.ok(Map.of("name", name, "size", fileService.getDirectorySize(name)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam String name) {
        try {
            fileService.deleteDirectory(name);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam String dir,
            @RequestParam("file") MultipartFile file) {
        try {
            fileService.uploadFile(dir, file.getOriginalFilename(), file.getInputStream());
            return ResponseEntity.ok(Map.of("message", "Uploaded", "file", file.getOriginalFilename()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}