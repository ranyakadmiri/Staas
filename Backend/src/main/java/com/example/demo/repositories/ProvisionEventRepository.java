package com.example.demo.repositories;

import com.example.demo.entities.ProvisionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProvisionEventRepository extends JpaRepository<ProvisionEvent, Long> {
    List<ProvisionEvent> findByVolumeIdOrderByCreatedAtAsc(Long volumeId);
}