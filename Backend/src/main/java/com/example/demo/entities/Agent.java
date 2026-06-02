package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long projectId;

    private String hostname;
    private String initiatorIqn;
    private String os;            // linux | windows | macos

    private LocalDateTime lastHeartbeat;
    private LocalDateTime registeredAt;
}