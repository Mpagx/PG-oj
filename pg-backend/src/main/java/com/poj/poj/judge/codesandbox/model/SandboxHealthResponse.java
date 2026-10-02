package com.poj.poj.judge.codesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SandboxHealthResponse {
    private String status;
    private String service;
    private String version;
    private List<String> supportedLanguages;
    private String message;

    public static SandboxHealthResponse down(String message) {
        return new SandboxHealthResponse("DOWN", "cookie-code-sandbox", "1.0.0",
                Collections.emptyList(), message);
    }
}
