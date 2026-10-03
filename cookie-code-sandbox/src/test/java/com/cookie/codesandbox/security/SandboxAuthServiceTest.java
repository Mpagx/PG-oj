package com.cookie.codesandbox.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SandboxAuthServiceTest {

    @Test
    void acceptsMatchingToken() {
        SandboxAuthService authService = new SandboxAuthService("secret-token");

        assertDoesNotThrow(() -> authService.verify("secret-token"));
    }

    @Test
    void rejectsMissingOrIncorrectToken() {
        SandboxAuthService authService = new SandboxAuthService("secret-token");

        assertThrows(ResponseStatusException.class, () -> authService.verify(null));
        assertThrows(ResponseStatusException.class, () -> authService.verify("wrong-token"));
    }
}
