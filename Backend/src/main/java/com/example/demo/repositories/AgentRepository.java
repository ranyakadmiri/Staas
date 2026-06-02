package com.example.demo.repositories;

import com.example.demo.entities.Agent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentRepository extends JpaRepository<Agent, Long> {
    List<Agent> findByProjectId(Long projectId);
}