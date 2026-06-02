package com.example.demo.controllers;

import com.example.demo.dto.AgentRegisterRequest;
import com.example.demo.dto.AgentRegisterResponse;
import com.example.demo.dto.CommandAckRequest;
import com.example.demo.security.JwtUtils;
import com.example.demo.services.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints consumed by the STaaS agent running on client VMs.
 *
 * Token types:
 *   POST /register   → user JWT (from /api/auth/login → /api/auth/verify-otp)
 *   all other routes → agent JWT (returned by /register)
 */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final JwtUtils     jwtUtils;      // your existing JwtUtils

    /**
     * POST /api/agents/register
     *
     * The user authenticates normally via /api/auth/login + /api/auth/verify-otp,
     * gets their JWT, then the agent uses that JWT here once to register itself.
     *
     * Returns a dedicated agent JWT for all subsequent polling calls.
     */
    @PostMapping("/register")
    public ResponseEntity<AgentRegisterResponse> register(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AgentRegisterRequest request) {

        String userJwt = extractToken(authHeader);

        // Validate it's a real user JWT using your existing JwtUtils
        if (!jwtUtils.isTokenValid(userJwt)) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(agentService.register(userJwt, request));
    }

    /**
     * GET /api/agents/me/commands
     * Agent polls this every 10s. Returns PENDING commands.
     * Requires agent JWT.
     */
    @GetMapping("/me/commands")
    public ResponseEntity<?> getPendingCommands(
            @RequestHeader("Authorization") String authHeader) {

        Long agentId = resolveAgentId(authHeader);
        return ResponseEntity.ok(agentService.getPendingCommands(agentId));
    }

    /**
     * POST /api/agents/me/commands/{id}/ack
     * Agent reports the result of a command execution.
     * Requires agent JWT.
     */
    @PostMapping("/me/commands/{id}/ack")
    public ResponseEntity<?> ackCommand(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody CommandAckRequest request) {

        Long agentId = resolveAgentId(authHeader);
        agentService.ackCommand(agentId, id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/agents/me/heartbeat
     * Updates the agent's last-seen timestamp.
     * Requires agent JWT.
     */
    @PostMapping("/me/heartbeat")
    public ResponseEntity<?> heartbeat(
            @RequestHeader("Authorization") String authHeader) {

        Long agentId = resolveAgentId(authHeader);
        agentService.heartbeat(agentId);
        return ResponseEntity.ok().build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Long resolveAgentId(String authHeader) {
        return agentService.validateAgentJwt(extractToken(authHeader));
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or malformed Authorization header");
        }
        return authHeader.substring(7);
    }
}