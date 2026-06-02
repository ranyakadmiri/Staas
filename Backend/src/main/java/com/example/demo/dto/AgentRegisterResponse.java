package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentRegisterResponse {
    private Long   agentId;
    private String jwt;
}
