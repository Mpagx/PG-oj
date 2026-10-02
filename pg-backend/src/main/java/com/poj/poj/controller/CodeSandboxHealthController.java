package com.poj.poj.controller;

import com.poj.poj.common.BaseResponse;
import com.poj.poj.common.ResultUtils;
import com.poj.poj.judge.codesandbox.impl.RemoteCodeSandbox;
import com.poj.poj.judge.codesandbox.model.SandboxHealthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/code-sandbox")
public class CodeSandboxHealthController {

    private final RemoteCodeSandbox remoteCodeSandbox;

    public CodeSandboxHealthController(RemoteCodeSandbox remoteCodeSandbox) {
        this.remoteCodeSandbox = remoteCodeSandbox;
    }

    @GetMapping("/health")
    public ResponseEntity<BaseResponse<SandboxHealthResponse>> health() {
        SandboxHealthResponse health = remoteCodeSandbox.health();
        HttpStatus status = "UP".equalsIgnoreCase(health.getStatus())
                ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(ResultUtils.success(health));
    }
}
