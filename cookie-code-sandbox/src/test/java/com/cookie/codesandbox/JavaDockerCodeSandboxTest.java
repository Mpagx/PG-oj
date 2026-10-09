package com.cookie.codesandbox;

import com.cookie.codesandbox.config.SandboxLimitsProperties;
import com.cookie.codesandbox.model.ExecuteCodeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaDockerCodeSandboxTest {

    private JavaDockerCodeSandbox sandbox;

    @BeforeEach
    void setUp() {
        SandboxLimitsProperties limits = new SandboxLimitsProperties();
        limits.setMaxCodeBytes(32);
        limits.setMaxTestCases(2);
        limits.setMaxInputBytesPerCase(8);
        limits.setMaxTotalInputBytes(12);
        sandbox = new JavaDockerCodeSandbox(limits);
    }

    @Test
    void acceptsValidRequestAndAddsDefaultInput() {
        ExecuteCodeRequest request = request("class Main {}", null);

        assertDoesNotThrow(() -> sandbox.validateRequest(request));
        assertEquals(Collections.singletonList(""), request.getInputList());
    }

    @Test
    void rejectsOversizedCode() {
        ExecuteCodeRequest request = request(
                "public class Main { public static void main(String[] args) {} }",
                Collections.singletonList(""));

        assertThrows(IllegalArgumentException.class, () -> sandbox.validateRequest(request));
    }

    @Test
    void rejectsTooManyTestCases() {
        ExecuteCodeRequest request = request(
                "class Main {}", Arrays.asList("1", "2", "3"));

        assertThrows(IllegalArgumentException.class, () -> sandbox.validateRequest(request));
    }

    @Test
    void rejectsOversizedTestInput() {
        ExecuteCodeRequest request = request(
                "class Main {}", Collections.singletonList("123456789"));

        assertThrows(IllegalArgumentException.class, () -> sandbox.validateRequest(request));
    }

    @Test
    void resolvesDefaultResourceLimit() {
        assertEquals(1000L,
                sandbox.limitedOrDefault("时间", null, 1000L, 100L, 5000L));
    }

    @Test
    void rejectsResourceLimitOutsideSandboxBoundary() {
        assertThrows(IllegalArgumentException.class,
                () -> sandbox.limitedOrDefault("时间", 10_001L, 1000L, 100L, 10_000L));
        assertThrows(IllegalArgumentException.class,
                () -> sandbox.limitedOrDefault("内存", 1024L, 262_144L, 16_384L, 524_288L));
    }

    @Test
    void explainsMissingDockerTunnelWithoutLeakingStackTrace() {
        RuntimeException error = new RuntimeException(
                "transport failed", new ConnectException("Connection refused: 127.0.0.1:2375"));

        assertEquals(
                "Docker Engine 无法连接，请运行 scripts/start-sandbox-vm.ps1 建立 2375 SSH 隧道",
                sandbox.infrastructureErrorMessage(error));
    }

    private ExecuteCodeRequest request(String code, java.util.List<String> inputs) {
        return ExecuteCodeRequest.builder()
                .language("java")
                .code(code)
                .inputList(inputs)
                .build();
    }
}
