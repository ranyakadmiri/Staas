package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentCommandResponse {
    private Long   id;
    private String type;           // ATTACH | DETACH
    private String targetIqn;
    private String portalAddress;
}