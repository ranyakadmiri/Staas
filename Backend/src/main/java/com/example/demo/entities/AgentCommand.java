package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_commands")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgentCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long agentId;

    /** ATTACH | DETACH */
    private String type;

    private String targetIqn;
    private String portalAddress;   // e.g. "192.168.100.51:3260"

    /** PENDING | DONE | FAILED */
    private String status;

    private String ackMessage;

    private LocalDateTime createdAt;
    private LocalDateTime ackedAt;
}