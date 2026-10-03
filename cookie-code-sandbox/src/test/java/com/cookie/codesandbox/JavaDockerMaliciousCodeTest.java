package com.cookie.codesandbox;

import com.cookie.codesandbox.config.SandboxLimitsProperties;
import com.cookie.codesandbox.model.ExecuteCodeRequest;
import com.cookie.codesandbox.model.ExecuteCodeResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 需要真实 Docker Engine 和 openjdk:8-alpine 镜像的攻击回归测试。
 *
 * <p>使用 {@code mvn -Dsandbox.docker.tests=true test} 显式运行，避免普通单元测试
 * 在未启动虚拟机时产生假失败。</p>
 */
@EnabledIfSystemProperty(named = "sandbox.docker.tests", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JavaDockerMaliciousCodeTest {

    private JavaDockerCodeSandbox sandbox;

    @BeforeAll
    void setUp() {
        SandboxLimitsProperties limits = new SandboxLimitsProperties();
        limits.setMaxOutputBytes(4096);
        limits.setMaxConcurrentExecutions(2);
        limits.setMaxTimeLimitMs(3000L);
        sandbox = new JavaDockerCodeSandbox(limits);

        assertEquals("UP", sandbox.health().getStatus(),
                "请先启动虚拟机 Docker Engine，并准备 openjdk:8-alpine 镜像");
    }

    @Test
    void terminatesInfiniteLoopAtDeadline() {
        ExecuteCodeResponse response = execute(
                "public class Main { public static void main(String[] args) {"
                        + " while (true) { } } }",
                500L, 131_072L);

        assertBlocked(response, "执行超时");
    }

    @Test
    void stopsOutputFloodWithoutGrowingServiceMemoryIndefinitely() {
        ExecuteCodeResponse response = execute(
                "public class Main { public static void main(String[] args) {"
                        + " while (true) { System.out.print(\"xxxxxxxxxxxxxxxx\"); } } }",
                1500L, 131_072L);

        assertBlocked(response, "输出超过限制");
    }

    @Test
    void deniesWritingToContainerRootFileSystem() {
        ExecuteCodeResponse response = execute(
                "import java.nio.file.*; public class Main {"
                        + " public static void main(String[] args) throws Exception {"
                        + " Files.write(Paths.get(\"/sandbox-escape-test\"),"
                        + " \"owned\".getBytes(\"UTF-8\")); } }",
                1500L, 131_072L);

        assertUserCodeFailed(response);
    }

    @Test
    void deniesOutboundNetworkConnection() {
        ExecuteCodeResponse response = execute(
                "import java.net.*; public class Main {"
                        + " public static void main(String[] args) throws Exception {"
                        + " Socket socket = new Socket();"
                        + " socket.connect(new InetSocketAddress(\"1.1.1.1\", 80), 300); } }",
                1500L, 131_072L);

        assertUserCodeFailed(response);
    }

    @Test
    void containsMemoryExhaustionInsideContainerLimit() {
        ExecuteCodeResponse response = execute(
                "import java.util.*; public class Main {"
                        + " public static void main(String[] args) {"
                        + " List<byte[]> values = new ArrayList<byte[]>();"
                        + " while (true) { values.add(new byte[1024 * 1024]); } } }",
                2500L, 65_536L);

        assertUserCodeFailed(response);
    }

    @Test
    void containsProcessStormWithPidLimit() {
        ExecuteCodeResponse response = execute(
                "public class Main { public static void main(String[] args) throws Exception {"
                        + " for (int i = 0; i < 200; i++) {"
                        + " new ProcessBuilder(\"sh\", \"-c\", \"sleep 30\").start();"
                        + " } } }",
                2500L, 131_072L);

        assertUserCodeFailed(response);
    }

    @Test
    void compilesAndRunsWithoutHostJavacExecution() {
        ExecuteCodeResponse response = execute(
                "public class Main { public static void main(String[] args) {"
                        + " System.out.print(\"compiled-in-container\"); } }",
                1500L, 131_072L);

        assertEquals("1", response.getStatus(), response.getMessage());
        assertEquals(Collections.singletonList("compiled-in-container"), response.getOutputList());
    }

    @Test
    void isolatesMutableStateBetweenTestCases() {
        ExecuteCodeResponse response = execute(
                "import java.nio.file.*; public class Main {"
                        + " public static void main(String[] args) throws Exception {"
                        + " Path state = Paths.get(\"/tmp/previous-case\");"
                        + " if (Files.exists(state)) throw new IllegalStateException(\"state leaked\");"
                        + " Files.write(state, args[0].getBytes(\"UTF-8\"));"
                        + " System.out.print(args[0]); } }",
                Arrays.asList("first", "second"), 1500L, 131_072L);

        assertEquals("1", response.getStatus(), response.getMessage());
        assertEquals(Arrays.asList("first", "second"), response.getOutputList());
    }

    private ExecuteCodeResponse execute(String code, long timeLimitMs, long memoryLimitKb) {
        return execute(code, Collections.singletonList(""), timeLimitMs, memoryLimitKb);
    }

    private ExecuteCodeResponse execute(
            String code, List<String> inputs, long timeLimitMs, long memoryLimitKb) {
        ExecuteCodeRequest request = ExecuteCodeRequest.builder()
                .protocolVersion("1.0")
                .requestId("malicious-test-" + UUID.randomUUID())
                .language("java")
                .code(code)
                .inputList(inputs)
                .timeLimitMs(timeLimitMs)
                .memoryLimitKb(memoryLimitKb)
                .stackLimitKb(1024L)
                .build();
        return sandbox.executeCode(request);
    }

    private void assertBlocked(ExecuteCodeResponse response, String expectedMessage) {
        assertUserCodeFailed(response);
        assertTrue(response.getMessage().contains(expectedMessage),
                "实际错误信息: " + response.getMessage());
    }

    private void assertUserCodeFailed(ExecuteCodeResponse response) {
        assertNotNull(response);
        assertEquals("3", response.getStatus(), "沙箱内部异常: " + response.getMessage());
        assertNotNull(response.getMessage());
    }
}
