package com.cookie.codesandbox.controller;

import com.cookie.codesandbox.model.SandboxHealthResponse;
import com.cookie.codesandbox.security.SandboxAuthService;
import com.cookie.codesandbox.JavaDockerCodeSandbox;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController("/")

public class MainController {

    private final SandboxAuthService authService;

    private final JavaDockerCodeSandbox codeSandbox;

    public MainController(SandboxAuthService authService, JavaDockerCodeSandbox codeSandbox) {
        this.authService = authService;
        this.codeSandbox = codeSandbox;
    }

    @GetMapping("/health")
    public ResponseEntity<SandboxHealthResponse> checkHealth(
            @RequestHeader(value = "X-Sandbox-Token", required = false) String token) {
        authService.verify(token);
        SandboxHealthResponse health = codeSandbox.health();
        HttpStatus status = "UP".equals(health.getStatus())
                ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(health);
    }
}
