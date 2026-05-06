package com.example.demo.controllers;

import com.example.demo.entities.User;
import com.example.demo.entities.UserStatus;
import com.example.demo.services.EmailService;
import com.example.demo.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final EmailService emailService;

    // 🔹 LIST PENDING USERS
    @GetMapping("/pending-users")
    public ResponseEntity<?> getPendingUsers() {
        return ResponseEntity.ok(userService.getPendingUsers());
    }

    // 🔹 APPROVE USER
    @PostMapping("/approve/{userId}")
    public ResponseEntity<?> approveUser(@PathVariable Long userId) {

        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(UserStatus.APPROVED);

        userService.createUser(user);

        // 🔥 send email
        emailService.sendApprovalEmail(user);

        return ResponseEntity.ok("User approved");
    }

    // 🔹 REJECT USER
    @PostMapping("/reject/{userId}")
    public ResponseEntity<?> rejectUser(@PathVariable Long userId) {

        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(UserStatus.REJECTED);

        userService.createUser(user);

        return ResponseEntity.ok("User rejected");
    }
}
