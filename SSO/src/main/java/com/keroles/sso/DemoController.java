package com.keroles.sso;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DemoController {

    @GetMapping("/public")
    public String publicEndpoint() {
        return "👋 Hello! This is a public endpoint. No login required.";
    }

    @GetMapping("/private")
    public Map<String, Object> privateEndpoint(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "message", "🔐 Welcome to the secure zone!",
                "userId", jwt.getSubject(),
                "name", jwt.getClaimAsString("name"),
                "email", jwt.getClaimAsString("email"),
                "preferredUsername", jwt.getClaimAsString("preferred_username")
        );
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('SCOPE_admin')") // Example: requiring 'admin' scope
    public String adminEndpoint() {
        return "🚀 Welcome, Admin! You have special powers.";
    }
}
