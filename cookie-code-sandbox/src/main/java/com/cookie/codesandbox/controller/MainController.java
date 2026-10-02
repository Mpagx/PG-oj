package com.cookie.codesandbox.controller;

import com.cookie.codesandbox.model.SandboxHealthResponse;
import com.cookie.codesandbox.security.SandboxAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController("/")

public class MainController {

    private final SandboxAuthService authService;

    public MainController(SandboxAuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/health")
    public SandboxHealthResponse checkHealth(
            @RequestHeader(value = "X-Sandbox-Token", required = false) String token) {
        authService.verify(token);
        return new SandboxHealthResponse("UP", "cookie-code-sandbox", "1.0.0",
                Collections.singletonList("java"), "ok");
    }
}
