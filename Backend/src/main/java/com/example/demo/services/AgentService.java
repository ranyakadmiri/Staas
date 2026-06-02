package com.example.demo.services;

import com.example.demo.dto.AgentCommandResponse;
import com.example.demo.dto.AgentRegisterRequest;
import com.example.demo.dto.AgentRegisterResponse;
import com.example.demo.dto.CommandAckRequest;
import com.example.demo.entities.Agent;
import com.example.demo.entities.AgentCommand;
import com.example.demo.repositories.AgentCommandRepository;
import com.example.demo.repositories.AgentRepository;
import com.example.demo.security.JwtUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository        agentRepository;
    private final AgentCommandRepository commandRepository;
    private final JwtUtils               jwtUtils;      // your existing JwtUtils
    private final UserService            userService;   // your existing UserService

    @Value("${staas.agent.jwt.secret}")
    private String agentJwtSecret;

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Called by AgentController after it has validated the user's JWT.
     *
     * Flow:
     *   1. User logs in normally → gets user JWT (email as subject)
     *   2. Agent calls POST /api/agents/register with that user JWT
     *   3. We extract the email from the JWT, look up the user's projectId,
     *      create the Agent record, and issue a separate agent JWT
     */
    public AgentRegisterResponse register(String userJwt, AgentRegisterRequest req) {

        // 1. Extract email from the user's existing JWT (uses your JwtUtils)
        String email = jwtUtils.extractUsername(userJwt);

        // 2. Resolve projectId from email using your existing UserService
        Long projectId = userService.findProjectIdByEmail(email);

        if (projectId == null) {
            throw new IllegalArgumentException(
                    "No project found for user: " + email +
                            " — create a project first before registering an agent.");
        }

        // 3. Persist the agent
        Agent agent = Agent.builder()
                .projectId(projectId)
                .hostname(req.getHostname())
                .initiatorIqn(req.getInitiatorIqn())
                .os(req.getOs())
                .registeredAt(LocalDateTime.now())
                .lastHeartbeat(LocalDateTime.now())
                .build();

        agentRepository.save(agent);

        // 4. Issue a dedicated agent JWT (subject = "agt:<agentId>")
        //    Separate from the user JWT so agent tokens can be revoked independently
        String agentJwt = buildAgentJwt(agent.getId());

        log.info("Agent registered — id={} hostname={} iqn={} project={}",
                agent.getId(), agent.getHostname(), agent.getInitiatorIqn(), projectId);

        return AgentRegisterResponse.builder()
                .agentId(agent.getId())
                .jwt(agentJwt)
                .build();
    }

    // ── Heartbeat ─────────────────────────────────────────────────────────────

    public void heartbeat(Long agentId) {
        agentRepository.findById(agentId).ifPresent(a -> {
            a.setLastHeartbeat(LocalDateTime.now());
            agentRepository.save(a);
        });
    }

    // ── Command queue ─────────────────────────────────────────────────────────

    public List<AgentCommandResponse> getPendingCommands(Long agentId) {
        return commandRepository.findByAgentIdAndStatus(agentId, "PENDING")
                .stream()
                .map(c -> AgentCommandResponse.builder()
                        .id(c.getId())
                        .type(c.getType())
                        .targetIqn(c.getTargetIqn())
                        .portalAddress(c.getPortalAddress())
                        .build())
                .toList();
    }

    public void enqueueAttach(Long agentId, String targetIqn, String portalAddress) {
        enqueue(agentId, "ATTACH", targetIqn, portalAddress);
    }

    public void enqueueDetach(Long agentId, String targetIqn, String portalAddress) {
        enqueue(agentId, "DETACH", targetIqn, portalAddress);
    }

    public void ackCommand(Long agentId, Long commandId, CommandAckRequest req) {
        AgentCommand cmd = commandRepository.findById(commandId)
                .orElseThrow(() -> new IllegalArgumentException("Command not found"));

        if (!cmd.getAgentId().equals(agentId)) {
            throw new IllegalArgumentException("Command does not belong to this agent");
        }

        cmd.setStatus(req.isSuccess() ? "DONE" : "FAILED");
        cmd.setAckMessage(req.getMessage());
        cmd.setAckedAt(LocalDateTime.now());
        commandRepository.save(cmd);

        log.info("Command #{} acked — agent={} success={} msg={}",
                commandId, agentId, req.isSuccess(), req.getMessage());
    }

    // ── Agent JWT ─────────────────────────────────────────────────────────────

    /**
     * Validates an agent JWT and returns the agent ID.
     * Agent tokens have subject "agt:<id>" to distinguish them from user tokens.
     */
    public Long validateAgentJwt(String jwt) {
        try {
            String subject = Jwts.parserBuilder()
                    .setSigningKey(agentSigningKey())
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody()
                    .getSubject();

            if (!subject.startsWith("agt:")) {
                throw new IllegalArgumentException("Not an agent token");
            }

            return Long.parseLong(subject.substring(4));

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or expired agent JWT: " + e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void enqueue(Long agentId, String type, String targetIqn, String portalAddress) {
        commandRepository.save(AgentCommand.builder()
                .agentId(agentId)
                .type(type)
                .targetIqn(targetIqn)
                .portalAddress(portalAddress)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build());
        log.info("Enqueued {} for agent {}: {}", type, agentId, targetIqn);
    }

    private String buildAgentJwt(Long agentId) {
        return Jwts.builder()
                .setSubject("agt:" + agentId)
                .setIssuedAt(new Date())
                // 1 year — long-lived so the agent doesn't need to re-register
                .setExpiration(new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000))
                .signWith(agentSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key agentSigningKey() {
        return Keys.hmacShaKeyFor(agentJwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}