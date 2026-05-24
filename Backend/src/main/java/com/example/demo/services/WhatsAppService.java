package com.example.demo.services;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class WhatsAppService {

    @Value("${twilio.account-sid}") private String accountSid;
    @Value("${twilio.auth-token}")  private String authToken;
    @Value("${twilio.whatsapp-from}") private String from;

    @PostConstruct
    public void init() {
        System.out.println("=== Twilio init: SID=" + accountSid + " FROM=" + from);
        Twilio.init(accountSid, authToken);
    }

    public void sendOtp(String toPhone, String otp) {
        try {
            System.out.println("=== Sending WhatsApp to: " + toPhone + " OTP: " + otp);
            Message message = Message.creator(
                            new PhoneNumber("whatsapp:" + toPhone),
                            new PhoneNumber(from),
                            "Your NextStep verification code is: " + otp + ". Expires in 5 minutes."
                    )
                    .setContentSid("HXb5b62575e6e4ff6129ad7c8efe1f983e")
                    .setContentVariables("{\"1\":\"" + otp + "\"}")
                    .create();
            System.out.println("=== Message SID: " + message.getSid());
            System.out.println("=== Message Status: " + message.getStatus());
        } catch (Exception e) {
            System.err.println("=== Twilio ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}