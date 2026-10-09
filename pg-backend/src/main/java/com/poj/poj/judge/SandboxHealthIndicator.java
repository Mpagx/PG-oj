package com.poj.poj.judge;

import com.poj.poj.judge.codesandbox.impl.RemoteCodeSandbox;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("sandbox")
public class SandboxHealthIndicator implements HealthIndicator {
    private final RemoteCodeSandbox sandbox;
    public SandboxHealthIndicator(RemoteCodeSandbox sandbox) { this.sandbox = sandbox; }
    @Override
    public Health health() {
        return "UP".equals(sandbox.health().getStatus()) ? Health.up().build() : Health.down().build();
    }
}
