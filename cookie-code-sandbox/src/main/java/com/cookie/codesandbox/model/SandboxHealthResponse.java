package com.cookie.codesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SandboxHealthResponse {
    private String status;
    private String service;
    private String version;
    private List<String> supportedLanguages;
    private String message;
}
