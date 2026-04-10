package com.example.demo.controllers;

import com.example.demo.services.BlockStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/block")
@RequiredArgsConstructor
public class BlockController {


        private final BlockStorageService blockService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) throws Exception {
        String name = (String) body.get("name");
        int sizeGB = (Integer) body.get("sizeGB");
        blockService.createVolume(name, sizeGB);
        return ResponseEntity.ok(Map.of("message", "Volume created", "name", name));
    }

        @GetMapping("/list")
        public ResponseEntity<?> list() throws Exception {
            return ResponseEntity.ok(blockService.listVolumes());
        }

        @DeleteMapping("/delete")
        public ResponseEntity<?> delete(@RequestParam String name) throws Exception {
            blockService.deleteVolume(name);
            return ResponseEntity.ok("Deleted");
        }
    @GetMapping("/info")
    public ResponseEntity<?> info(@RequestParam String name) throws Exception {
        return ResponseEntity.ok(Map.of("info", blockService.getVolumeInfo(name)));
    }
    }