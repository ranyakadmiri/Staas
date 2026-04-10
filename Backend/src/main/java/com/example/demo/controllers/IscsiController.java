package com.example.demo.controllers;

import com.example.demo.dto.IscsiVolumeDTO;
import com.example.demo.services.IscsiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/iscsi")
@RequiredArgsConstructor
public class IscsiController {

    private final IscsiService iscsiService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody IscsiVolumeDTO dto) {
        String result = iscsiService.createIscsiVolume(dto);
        return ResponseEntity.ok(Map.of(
                "message", "iSCSI volume created",
                "details", result,
                "esxi", iscsiService.buildEsxiHelp(dto.getName())
        ));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam String name,
                                    @RequestParam String initiatorIqn) {
        String result = iscsiService.deleteIscsiVolume(name, initiatorIqn);
        return ResponseEntity.ok(Map.of(
                "message", "iSCSI volume deleted",
                "details", result
        ));
    }

    @GetMapping("/targets")
    public ResponseEntity<?> targets() {
        return ResponseEntity.ok(Map.of(
                "targets", iscsiService.listTargets()
        ));
    }

    @GetMapping("/disks")
    public ResponseEntity<?> disks() {
        return ResponseEntity.ok(Map.of(
                "disks", iscsiService.listDisks()
        ));
    }

    @GetMapping("/esxi-help")
    public ResponseEntity<?> esxiHelp(@RequestParam String name) {
        return ResponseEntity.ok(Map.of(
                "help", iscsiService.buildEsxiHelp(name)
        ));
    }
}