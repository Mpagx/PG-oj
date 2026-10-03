package com.cookie.codesandbox.controller;

import com.cookie.codesandbox.JavaDockerCodeSandbox;
import com.cookie.codesandbox.model.ExecuteCodeResponse;
import com.cookie.codesandbox.model.JudgeInfo;
import com.cookie.codesandbox.model.SandboxHealthResponse;
import com.cookie.codesandbox.security.SandboxAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CodeSandboxController.class, MainController.class})
@Import(SandboxAuthService.class)
@TestPropertySource(properties = "codesandbox.token=integration-secret")
class SandboxControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JavaDockerCodeSandbox codeSandbox;

    @Test
    void rejectsExecuteRequestWithoutTokenBeforeRunningCode() throws Exception {
        mockMvc.perform(post("/executeCode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(codeSandbox);
    }

    @Test
    void executesAuthorizedRequestAndEchoesProtocolIdentity() throws Exception {
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage("执行成功");
        ExecuteCodeResponse sandboxResponse = ExecuteCodeResponse.builder()
                .status("1")
                .outputList(Collections.singletonList("3"))
                .judgeInfo(judgeInfo)
                .build();
        when(codeSandbox.executeCode(any())).thenReturn(sandboxResponse);

        mockMvc.perform(post("/executeCode")
                        .header("X-Sandbox-Token", "integration-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolVersion").value("1.0"))
                .andExpect(jsonPath("$.requestId").value("integration-request"))
                .andExpect(jsonPath("$.status").value("1"))
                .andExpect(jsonPath("$.outputList[0]").value("3"));

        verify(codeSandbox).executeCode(any());
    }

    @Test
    void returnsServiceUnavailableWhenDockerDependencyIsDown() throws Exception {
        when(codeSandbox.health()).thenReturn(new SandboxHealthResponse(
                "DOWN", "cookie-code-sandbox", "1.0.0", Collections.emptyList(),
                "Docker Engine 或执行镜像不可用"));

        mockMvc.perform(get("/health")
                        .header("X-Sandbox-Token", "integration-secret"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void returnsOkOnlyWhenExecutionDependenciesAreHealthy() throws Exception {
        when(codeSandbox.health()).thenReturn(new SandboxHealthResponse(
                "UP", "cookie-code-sandbox", "1.0.0",
                Collections.singletonList("java"), "ok"));

        mockMvc.perform(get("/health")
                        .header("X-Sandbox-Token", "integration-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.supportedLanguages[0]").value("java"));
    }

    private String validRequestJson() {
        return "{"
                + "\"protocolVersion\":\"1.0\","
                + "\"requestId\":\"integration-request\","
                + "\"language\":\"java\","
                + "\"code\":\"public class Main {}\","
                + "\"inputList\":[\"1 2\"]"
                + "}";
    }
}
