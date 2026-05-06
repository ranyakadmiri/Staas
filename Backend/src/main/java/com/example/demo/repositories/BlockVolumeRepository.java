package com.example.demo.repositories;

import com.example.demo.entities.BlockVolume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlockVolumeRepository extends JpaRepository<BlockVolume, Long> {
    List<BlockVolume> findByProjectId(Long projectId);
    Optional<BlockVolume> findByProjectIdAndName(Long projectId, String name);
    boolean existsByProjectIdAndName(Long projectId, String name);
}