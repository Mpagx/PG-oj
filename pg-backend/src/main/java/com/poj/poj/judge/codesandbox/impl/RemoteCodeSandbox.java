package com.poj.poj.judge.codesandbox.impl;

import com.poj.poj.judge.codesandbox.CodeSandbox;
import com.poj.poj.judge.codesandbox.model.ExecuteCodeRequest;
import com.poj.poj.judge.codesandbox.model.ExecuteCodeResponse;
import com.poj.poj.judge.codesandbox.model.SandboxHealthResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Objects;

/** 调用现有 cookie-code-sandbox 的 HTTP 客户端。 */
@Component
public class RemoteCodeSandbox implements CodeSandbox {

    private static final String TOKEN_HEADER = "X-Sandbox-Token";

    private final RestTemplate restTemplate;

    private RestTemplate healthRestTemplate;

    private final String baseUrl;

    private final String token;

    public RemoteCodeSandbox() {
        this(createRestTemplate(2000, 10000),
                readSetting("CODE_SANDBOX_BASE_URL", "http://127.0.0.1:8090"),
                readSetting("CODE_SANDBOX_TOKEN", ""));
    }

    @Autowired
    public RemoteCodeSandbox(
            @Value("${codesandbox.base-url:http://127.0.0.1:8090}") String baseUrl,
            @Value("${codesandbox.token:}") String token,
            @Value("${codesandbox.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${codesandbox.read-timeout-ms:10000}") int readTimeoutMs) {
        this(createRestTemplate(connectTimeoutMs, readTimeoutMs), baseUrl, token);
        // Readiness must not inherit the long execution timeout (up to 660 seconds).
        this.healthRestTemplate = createRestTemplate(Math.min(connectTimeoutMs, 2000), 3000);
    }

    public RemoteCodeSandbox(RestTemplate restTemplate, String baseUrl, String token) {
        this.restTemplate = restTemplate;
        this.healthRestTemplate = restTemplate;
        this.baseUrl = StringUtils.removeEnd(baseUrl, "/");
        this.token = token;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        if (executeCodeRequest == null) {
            throw new IllegalArgumentException("代码沙箱请求不能为空");
        }
        try {
            ResponseEntity<ExecuteCodeResponse> response = restTemplate.exchange(
                    baseUrl + "/executeCode", HttpMethod.POST,
                    new HttpEntity<>(executeCodeRequest, headers()), ExecuteCodeResponse.class);
            ExecuteCodeResponse body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null || body.getStatus() == null) {
                throw new IllegalStateException("代码沙箱返回了无效响应");
            }
            if (!Objects.equals(executeCodeRequest.getProtocolVersion(), body.getProtocolVersion())) {
                throw new IllegalStateException("代码沙箱协议版本不匹配");
            }
            if (!Objects.equals(executeCodeRequest.getRequestId(), body.getRequestId())) {
                throw new IllegalStateException("代码沙箱响应 requestId 不匹配");
            }
            if (!java.util.Arrays.asList("1", "2", "3").contains(body.getStatus())) throw new IllegalStateException("沙箱状态无效");
            return body;

        } catch (RestClientException e) {
            throw new IllegalStateException("调用代码沙箱失败", e);
        }
    }

    public SandboxHealthResponse health() {
        try {
            ResponseEntity<SandboxHealthResponse> response = healthRestTemplate.exchange(
                    baseUrl + "/health", HttpMethod.GET,
                    new HttpEntity<>(headers()), SandboxHealthResponse.class);
            SandboxHealthResponse body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                throw new IllegalStateException("代码沙箱健康检查响应无效");
            }
            return body;
        } catch (RestClientException | IllegalStateException e) {
            return SandboxHealthResponse.down("无法连接代码沙箱: " + e.getClass().getSimpleName());
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(token)) {
            headers.set(TOKEN_HEADER, token);
        }
        return headers;
    }

    private static RestTemplate createRestTemplate(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }

    private static String readSetting(String name, String defaultValue) {
        String value = System.getenv(name);
        return StringUtils.isBlank(value) ? defaultValue : value;
    }
}
