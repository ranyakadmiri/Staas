package com.example.demo.repositories;

import com.example.demo.entities.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileShareRepository extends JpaRepository<FileShare, Long> {

    List<FileShare> findByProjectId(Long projectId);

    Optional<FileShare> findByProjectIdAndName(Long projectId, String name);

    boolean existsByProjectIdAndName(Long projectId, String name);

    Optional<FileShare> findTopByOrderByExportIdDesc();
}