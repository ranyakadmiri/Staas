package com.example.demo.dto;

import lombok.Data;

@Data
public class AttachRequest {
    /** ID of the registered agent (from the agents table) */
    private Long agentId;
}