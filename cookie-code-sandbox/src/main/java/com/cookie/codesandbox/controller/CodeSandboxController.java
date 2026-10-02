package com.cookie.codesandbox.controller;

import com.cookie.codesandbox.JavaDockerCodeSandbox;
import com.cookie.codesandbox.model.ExecuteCodeRequest;
import com.cookie.codesandbox.model.ExecuteCodeResponse;
import com.cookie.codesandbox.security.SandboxAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 代码沙箱接口
 */
@RestController
public class CodeSandboxController {

    private final JavaDockerCodeSandbox codeSandbox;

    private final SandboxAuthService authService;

    public CodeSandboxController(JavaDockerCodeSandbox codeSandbox, SandboxAuthService authService) {
        this.codeSandbox = codeSandbox;
        this.authService = authService;
    }

    /**
     * 调用 Docker 代码沙箱执行代码。
     *
     * @param executeCodeRequest 执行代码请求
     * @return 执行结果
     */
    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(
            @RequestBody ExecuteCodeRequest executeCodeRequest,
            @RequestHeader(value = "X-Sandbox-Token", required = false) String token) {
        authService.verify(token);
        ExecuteCodeResponse response = codeSandbox.executeCode(executeCodeRequest);
        response.setProtocolVersion("1.0");
        response.setRequestId(executeCodeRequest.getRequestId());
        return response;
    }
}
