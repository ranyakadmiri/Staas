package com.example.demo.entities;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OtpStore {
    // token → {email, otp, expiry}
    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

    public String generateAndStore(String email) {
        String otp = String.valueOf((int)(Math.random() * 900000) + 100000);
        String token = UUID.randomUUID().toString();
        store.put(token, new OtpEntry(email, otp, Instant.now().plusSeconds(300)));
        return token; // return to client as a "session" reference
    }

    public String getOtp(String token) {
        OtpEntry e = store.get(token);
        return (e != null && Instant.now().isBefore(e.expiry())) ? e.otp() : null;
    }

    public String getEmail(String token) {
        OtpEntry e = store.get(token);
        return (e != null) ? e.email() : null;
    }

    public void invalidate(String token) { store.remove(token); }

    public record OtpEntry(String email, String otp, Instant expiry) {}
}
