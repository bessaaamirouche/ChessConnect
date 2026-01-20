package com.chessconnect.controller;

import com.chessconnect.dto.contact.ContactAdminRequest;
import com.chessconnect.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/contact")
public class ContactController {

    private final EmailService emailService;

    @Value("${app.admin.email:bessaa.amirouche@gmail.com}")
    private String adminEmail;

    public ContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/admin")
    public ResponseEntity<Map<String, String>> contactAdmin(@Valid @RequestBody ContactAdminRequest request) {
        emailService.sendContactAdminEmail(
                adminEmail,
                request.name(),
                request.email(),
                request.subject(),
                request.message()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Votre message a ete envoye. L'administrateur vous repondra dans les plus brefs delais."
        ));
    }
}
