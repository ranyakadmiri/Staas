package com.example.demo.controllers;

import com.example.demo.services.ObjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/objects")
@RequiredArgsConstructor
public class ObjectController {

    private final ObjectService objectService;

    @GetMapping("/{projectId}/{bucket}")
    public List<Map<String,Object>> listObjects(
            @PathVariable Long projectId,
            @PathVariable String bucket){

        return objectService.listObjects(projectId, bucket);
    }
}
