package com.example.demo.dto;

import lombok.Data;

@Data
public class AgentRegisterRequest {
    private String hostname;
    private String initiatorIqn;
    private String os;             // linux | windows | macos

}
