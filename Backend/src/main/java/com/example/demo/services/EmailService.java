package com.example.demo.services;

import com.example.demo.entities.User;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    public void sendApprovalEmail(User user) {

        // 🔥 For now just log (replace later with Twilio)
        System.out.println("Sending email to: " + user.getEmail());

        System.out.println("""
        ==========================
        ACCOUNT APPROVED
        ==========================
        Email: %s
        Password: (user already set)
        You can now login.
        ==========================
        """.formatted(user.getEmail()));

    }
}