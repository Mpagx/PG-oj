package com.cookie.codesandbox;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DockerClientBuilderTest {

    @Test
    void allowsPlaintextDockerOnlyOnLoopback() {
        assertDoesNotThrow(() -> DockerClientBuilder.validateDockerEndpoint(
                URI.create("tcp://127.0.0.1:2375"), false));
        assertDoesNotThrow(() -> DockerClientBuilder.validateDockerEndpoint(
                URI.create("tcp://localhost:2375"), false));
    }

    @Test
    void rejectsPlaintextRemoteDockerApi() {
        assertThrows(IllegalStateException.class,
                () -> DockerClientBuilder.validateDockerEndpoint(
                        URI.create("tcp://192.168.10.20:2375"), false));
    }

    @Test
    void allowsTlsProtectedRemoteDockerApi() {
        assertDoesNotThrow(() -> DockerClientBuilder.validateDockerEndpoint(
                URI.create("tcp://192.168.10.20:2376"), true));
    }
}
