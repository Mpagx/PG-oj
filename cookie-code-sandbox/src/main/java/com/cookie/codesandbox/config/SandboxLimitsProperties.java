package com.cookie.codesandbox.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 沙箱服务自身的硬上限。
 *
 * <p>题目配置只能在这些边界内申请资源，避免调用方通过伪造或错误配置
 * 放大虚拟机上的 CPU、内存、进程数和输出占用。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "codesandbox.limits")
public class SandboxLimitsProperties {

    private int maxCodeBytes = 64 * 1024;

    private int maxTestCases = 50;

    private int maxInputBytesPerCase = 16 * 1024;

    private int maxTotalInputBytes = 256 * 1024;

    private long minTimeLimitMs = 100L;

    private long maxTimeLimitMs = 10_000L;

    private long minMemoryLimitKb = 16L * 1024;

    private long maxMemoryLimitKb = 512L * 1024;

    private long minStackLimitKb = 256L;

    private long maxStackLimitKb = 64L * 1024;

    private int maxOutputBytes = 1024 * 1024;

    private long compileTimeoutMs = 10_000L;

    private int maxConcurrentExecutions = 4;

    private long queueWaitTimeoutMs = 1_000L;
}
