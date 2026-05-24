package com.example.demo.controllers;
import com.example.demo.entities.OtpStore;
import com.example.demo.entities.Role;
import com.example.demo.entities.User;
import com.example.demo.entities.UserStatus;
import com.example.demo.security.JwtUtils;
import com.example.demo.services.UserService;
import com.example.demo.services.WhatsAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private OtpStore otpStore;

    @Autowired
    private WhatsAppService whatsAppService;
    @Autowired
    private UserService userService;
    @GetMapping("/GetUserIdByEmail")
    public ResponseEntity<Long> getUserIdByEmail(@RequestParam String email) {
        Long userId = userService.findUserIdByEmail(email);
        return userId != null ? ResponseEntity.ok(userId) : ResponseEntity.notFound().build();
    }
   /* @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        User dbUser = userService.findUserByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (dbUser.getStatus() != UserStatus.APPROVED) {
            return ResponseEntity.status(403).body("Account not approved yet");
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
            );
            String token = jwtUtils.generateToken(user.getEmail());
            Map<String, String> response = new HashMap<>();
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "email", user.getEmail()
            ));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }*/
// Step 1: validate credentials → send OTP
   @PostMapping("/login")
   public ResponseEntity<?> login(@RequestBody User user) {
       User dbUser = userService.findUserByEmail(user.getEmail())
               .orElseThrow(() -> new RuntimeException("User not found"));

       if (dbUser.getStatus() != UserStatus.APPROVED)
           return ResponseEntity.status(403).body("Account not approved yet");

       try {
           authenticationManager.authenticate(
                   new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
           );
       } catch (AuthenticationException e) {
           return ResponseEntity.status(401).body("Invalid credentials");
       }

       String mfaToken = otpStore.generateAndStore(user.getEmail());
       // store the OTP separately so we can send it
       String otp = otpStore.getOtp(mfaToken); // grab before it's hashed/hidden if needed
       whatsAppService.sendOtp(dbUser.getPhone(), otp);

       return ResponseEntity.ok(Map.of(
               "mfaRequired", true,
               "mfaToken", mfaToken  // opaque reference, not the OTP itself
       ));
   }

    // Step 2: verify OTP → return JWT
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String mfaToken = body.get("mfaToken");
        String otp      = body.get("otp");
        System.out.println("=== MFA Token: " + mfaToken);
        System.out.println("=== OTP: " + otp);

        String storedOtp = otpStore.getOtp(mfaToken);
        String email     = otpStore.getEmail(mfaToken);

        if (storedOtp == null || !storedOtp.equals(otp))
            return ResponseEntity.status(401).body("Invalid or expired code");

        otpStore.invalidate(mfaToken);
        String jwt = jwtUtils.generateToken(email);

        return ResponseEntity.ok(Map.of("token", jwt, "email", email));
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));

        user.setStatus(UserStatus.PENDING); // 🔥 important
        user.setRole(Role.CLIENT);

        userService.createUser(user);

        return ResponseEntity.ok(Map.of(
                "message", "Registration submitted. Waiting for admin approval."
        ));
    }
}
