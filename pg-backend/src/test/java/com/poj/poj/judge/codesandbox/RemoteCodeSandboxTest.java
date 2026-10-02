package com.poj.poj.judge.codesandbox;

import com.poj.poj.judge.codesandbox.impl.RemoteCodeSandbox;
import com.poj.poj.judge.codesandbox.model.ExecuteCodeRequest;
import com.poj.poj.judge.codesandbox.model.ExecuteCodeResponse;
import com.poj.poj.judge.codesandbox.model.SandboxHealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RemoteCodeSandboxTest {

    private MockRestServiceServer server;
    private RemoteCodeSandbox sandbox;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        sandbox = new RemoteCodeSandbox(restTemplate, "http://sandbox.test", "secret");
    }

    @Test
    void executesAgainstExistingSandboxEndpoint() {
        server.expect(requestTo("http://sandbox.test/executeCode"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sandbox-Token", "secret"))
                .andRespond(withSuccess(
                        "{\"protocolVersion\":\"1.0\",\"requestId\":\"question-submit-1\","
                                + "\"outputList\":[\"3\"],\"status\":\"1\",\"judgeInfo\":{}}",
                        MediaType.APPLICATION_JSON));

        ExecuteCodeRequest request = ExecuteCodeRequest.builder()
                .protocolVersion("1.0")
                .requestId("question-submit-1")
                .language("java")
                .code("public class Main {}")
                .inputList(Collections.singletonList("1 2"))
                .build();
        ExecuteCodeResponse response = sandbox.executeCode(request);

        assertEquals("1", response.getStatus());
        assertEquals("3", response.getOutputList().get(0));
        server.verify();
    }

    @Test
    void reportsHealthAndRejectsNullExecutionBody() {
        server.expect(requestTo("http://sandbox.test/health"))
                .andRespond(withSuccess(
                        "{\"status\":\"UP\",\"service\":\"cookie-code-sandbox\","
                                + "\"version\":\"1.0.0\",\"supportedLanguages\":[\"java\"]}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://sandbox.test/executeCode"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        SandboxHealthResponse health = sandbox.health();
        assertEquals("UP", health.getStatus());
        assertThrows(IllegalStateException.class,
                () -> sandbox.executeCode(ExecuteCodeRequest.builder().language("java").build()));
        server.verify();
    }
}
