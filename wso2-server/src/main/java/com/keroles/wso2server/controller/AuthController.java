package com.keroles.wso2server.controller;

import com.keroles.wso2server.dto.LoginRequest;
import com.keroles.wso2server.dto.LoginResponse;
import com.keroles.wso2server.auth.security.JWTService;
import com.keroles.wso2server.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final JWTService jwtService;

    public AuthController(UserRepository userRepository, JWTService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return userRepository.findByUsername(request.username())
                .filter(user -> user.password().equals(request.password()))
                .map(user -> ResponseEntity.ok(new LoginResponse(jwtService.generateToken(user.username()))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
