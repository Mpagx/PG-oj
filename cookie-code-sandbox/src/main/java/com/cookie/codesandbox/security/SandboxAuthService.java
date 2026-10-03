package com.cookie.codesandbox.security;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class SandboxAuthService {

    private final String expectedToken;

    public SandboxAuthService(@Value("${codesandbox.token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    public void verify(String actualToken) {
        if (StringUtils.isNotBlank(expectedToken) && !constantTimeEquals(expectedToken, actualToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "沙箱访问令牌无效");
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = StringUtils.defaultString(actual).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
}
