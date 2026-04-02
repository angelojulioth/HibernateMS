package com.example.orderservice.integration.testauth;

import com.example.orderservice.infrastructure.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class TestAuthController {

    private final JwtService jwtService;

    public TestAuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body("User registered");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> request) {
        String username = request.get("username").toString();
        @SuppressWarnings("unchecked")
        Collection<?> rolesObj = (Collection<?>) request.getOrDefault("roles", Set.of("ROLE_USER"));
        Set<String> roles = rolesObj.stream().map(Object::toString).collect(Collectors.toSet());

        String token = jwtService.generateToken(
                User.builder()
                        .username(username)
                        .password("test")
                        .authorities(roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()))
                        .build());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", username,
                "expiresIn", 86400000));
    }
}
