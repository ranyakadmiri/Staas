package com.example.demo.repositories;

import com.example.demo.entities.AgentCommand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentCommandRepository extends JpaRepository<AgentCommand, Long> {
    List<AgentCommand> findByAgentIdAndStatus(Long agentId, String status);
}