package com.cookie.codesandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.cookie.codesandbox.config.SandboxLimitsProperties;
import com.cookie.codesandbox.model.ExecuteCodeRequest;
import com.cookie.codesandbox.model.ExecuteCodeResponse;
import com.cookie.codesandbox.model.ExecuteMessage;
import com.cookie.codesandbox.model.JudgeInfo;
import com.cookie.codesandbox.model.SandboxHealthResponse;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateVolumeResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 使用远程 Docker Engine 执行 Java 代码。
 *
 * <p>镜像由运维提前准备，执行请求不会访问镜像仓库。Windows 上生成的代码通过
 * Docker archive API 上传到容器，因此 Docker Engine 位于 Linux 虚拟机时也能工作。</p>
 */
@Service
@Slf4j
public class JavaDockerCodeSandbox implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    private static final String CONTAINER_CODE_DIR = "/app";
    private static final String DEFAULT_IMAGE = "openjdk:8-alpine";
    private static final long TIME_OUT_MILLIS = 5000L;
    private static final long DEFAULT_MEMORY_LIMIT_KB = 256L * 1024;

    private static final long DEFAULT_STACK_LIMIT_KB = 64L * 1024;

    private static final String SANDBOX_USER = "65534:65534";

    private static final long COMPILER_MEMORY_LIMIT_BYTES = 320L * 1024 * 1024;

    private final SandboxLimitsProperties limits;

    private final Semaphore executionSlots;

    public JavaDockerCodeSandbox(SandboxLimitsProperties limits) {
        this.limits = limits;
        this.executionSlots = new Semaphore(
                Math.max(1, limits.getMaxConcurrentExecutions()), true);
    }

    /**
     * 健康检查必须覆盖真正的执行依赖，避免仅 Web 端口存活就误报可用。
     */
    public SandboxHealthResponse health() {
        DockerClient dockerClient = null;
        try {
            dockerClient = DockerClientBuilder.getInstance().build();
            dockerClient.pingCmd().exec();
            ensureImageExists(dockerClient,
                    System.getProperty("codesandbox.docker.image", DEFAULT_IMAGE));
            return new SandboxHealthResponse("UP", "cookie-code-sandbox", "1.0.0",
                    Collections.singletonList("java"), "ok");
        } catch (Exception e) {
            log.warn("代码沙箱健康检查失败: {}", e.getClass().getSimpleName());
            return new SandboxHealthResponse("DOWN", "cookie-code-sandbox", "1.0.0",
                    Collections.emptyList(), "Docker Engine 或执行镜像不可用");
        } finally {
            closeDockerClientQuietly(dockerClient);
        }
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
        File requestDirectory = null;
        DockerClient dockerClient = null;
        String codeVolumeName = null;
        boolean executionSlotAcquired = false;

        try {
            validateRequest(request);
            executionSlotAcquired = acquireExecutionSlot();
            long timeoutMillis = limitedOrDefault("时间", request.getTimeLimitMs(),
                    TIME_OUT_MILLIS, limits.getMinTimeLimitMs(), limits.getMaxTimeLimitMs());
            long memoryLimitKb = limitedOrDefault("内存", request.getMemoryLimitKb(),
                    DEFAULT_MEMORY_LIMIT_KB, limits.getMinMemoryLimitKb(),
                    limits.getMaxMemoryLimitKb());
            long stackLimitKb = limitedOrDefault("栈", request.getStackLimitKb(),
                    DEFAULT_STACK_LIMIT_KB, limits.getMinStackLimitKb(),
                    limits.getMaxStackLimitKb());

            // 每次请求使用独立目录。本地目录名为 app，归档上传后正好对应容器 /app。
            File globalCodeDirectory = FileUtil.mkdir(
                    new File(System.getProperty("user.dir"), GLOBAL_CODE_DIR_NAME));
            requestDirectory = new File(globalCodeDirectory, UUID.randomUUID().toString());
            File localCodeDirectory = new File(requestDirectory, "app");
            FileUtil.writeString(
                    request.getCode(),
                    new File(localCodeDirectory, GLOBAL_JAVA_CLASS_NAME),
                    StandardCharsets.UTF_8);

            dockerClient = DockerClientBuilder.getInstance().build();
            String image = System.getProperty("codesandbox.docker.image", DEFAULT_IMAGE);
            ensureImageExists(dockerClient, image);

            // 用临时 volume 把 Windows 侧代码传到远程 Linux Docker。
            codeVolumeName = "sandbox-code-" + UUID.randomUUID();
            CreateVolumeResponse codeVolume = dockerClient.createVolumeCmd()
                    .withName(codeVolumeName)
                    .withLabels(Collections.singletonMap("owner", "cookie-code-sandbox"))
                    .exec();
            codeVolumeName = codeVolume.getName();
            uploadSource(dockerClient, image, codeVolumeName, localCodeDirectory);

            ExecuteMessage compileResult = compileInContainer(
                    dockerClient, image, codeVolumeName);
            if (compileResult.getExitValue() == null || compileResult.getExitValue() != 0) {
                ExecuteCodeResponse failure = userCodeErrorResponse(StrUtil.blankToDefault(
                        compileResult.getErrorMessage(), "代码编译失败"), compileResult.getTime());
                failure.setVerdict("CE");
                return failure;
            }

            return runTestCases(dockerClient, image, codeVolumeName,
                    request.getInputList(), timeoutMillis, memoryLimitKb, stackLimitKb);
        } catch (Throwable e) {
            return getErrorResponse(e, request == null ? null : request.getRequestId());
        } finally {
            removeVolumeQuietly(dockerClient, codeVolumeName);
            closeDockerClientQuietly(dockerClient);
            if (requestDirectory != null) {
                FileUtil.del(requestDirectory);
            }
            if (executionSlotAcquired) {
                executionSlots.release();
            }
        }
    }

    private void ensureImageExists(DockerClient dockerClient, String image) {
        try {
            dockerClient.inspectImageCmd(image).exec();
        } catch (NotFoundException e) {
            throw new IllegalStateException(
                    "Docker 镜像 " + image + " 不存在，请先在虚拟机中拉取镜像", e);
        }
    }

    private void uploadSource(
            DockerClient dockerClient, String image, String codeVolumeName,
            File localCodeDirectory) throws Exception {
        String helperContainerId = null;
        try {
            helperContainerId = createIdleContainer(dockerClient, image, codeVolumeName,
                    AccessMode.rw, 64L * 1024 * 1024, 16L, 16, null);
            dockerClient.copyArchiveToContainerCmd(helperContainerId)
                    .withHostResource(localCodeDirectory.getAbsolutePath())
                    .withRemotePath(CONTAINER_CODE_DIR)
                    .withDirChildrenOnly(true)
                    .exec();

            ExecuteMessage permissionResult = executeContainerCommand(
                    dockerClient, helperContainerId,
                    new String[]{"chmod", "-R", "a+rwX", CONTAINER_CODE_DIR},
                    5000L, false);
            if (permissionResult.getExitValue() == null
                    || permissionResult.getExitValue() != 0) {
                throw new IllegalStateException("无法准备容器代码目录");
            }
        } finally {
            removeContainerQuietly(dockerClient, helperContainerId);
        }
    }

    private ExecuteMessage compileInContainer(
            DockerClient dockerClient, String image, String codeVolumeName) throws Exception {
        String compilerContainerId = null;
        try {
            compilerContainerId = createIdleContainer(dockerClient, image, codeVolumeName,
                    AccessMode.rw, COMPILER_MEMORY_LIMIT_BYTES, 64L, 64, SANDBOX_USER);
            return executeContainerCommand(dockerClient, compilerContainerId,
                    new String[]{"javac", "-J-Xmx192m", "-J-Xss2m", "-proc:none",
                            "-encoding", "UTF-8", "-source", "8", "-target", "8",
                            GLOBAL_JAVA_CLASS_NAME},
                    limits.getCompileTimeoutMs(), true);
        } finally {
            removeContainerQuietly(dockerClient, compilerContainerId);
        }
    }

    private String createIdleContainer(
            DockerClient dockerClient, String image, String codeVolumeName,
            AccessMode accessMode, long memoryLimitBytes, long pidsLimit,
            int tmpSizeMb, String user) {
        HostConfig hostConfig = new HostConfig()
                .withMemory(memoryLimitBytes)
                .withMemorySwap(memoryLimitBytes)
                .withNanoCPUs(1_000_000_000L)
                .withPidsLimit(pidsLimit)
                .withReadonlyRootfs(true)
                .withCapDrop(Capability.ALL)
                .withSecurityOpts(Collections.singletonList("no-new-privileges:true"))
                .withBinds(new Bind(
                        codeVolumeName, new Volume(CONTAINER_CODE_DIR), accessMode))
                .withTmpFs(Collections.singletonMap(
                        "/tmp", "rw,noexec,nosuid,nodev,size=" + tmpSizeMb + "m,mode=1777"));

        com.github.dockerjava.api.command.CreateContainerCmd createCommand =
                dockerClient.createContainerCmd(image)
                        .withHostConfig(hostConfig)
                        .withNetworkDisabled(true)
                        .withWorkingDir(CONTAINER_CODE_DIR)
                        .withLabels(Collections.singletonMap("owner", "cookie-code-sandbox"))
                        .withAttachStderr(true)
                        .withAttachStdout(true)
                        .withTty(false)
                        .withCmd("sh", "-c", "while true; do sleep 3600; done");
        if (StrUtil.isNotBlank(user)) {
            createCommand.withUser(user);
        }
        CreateContainerResponse container = createCommand.exec();
        dockerClient.startContainerCmd(container.getId()).exec();
        return container.getId();
    }

    private ExecuteCodeResponse runTestCases(
            DockerClient dockerClient, String image, String codeVolumeName,
            List<String> inputList,
            long timeoutMillis, long memoryLimitKb, long stackLimitKb) throws Exception {
        List<ExecuteMessage> executeMessages = new ArrayList<>();
        long maxTime = 0L;
        long maxMemory = 0L;

        for (String input : inputList) {
            ExecuteMessage result = runOneTestCase(
                    dockerClient, image, codeVolumeName, input,
                    timeoutMillis, memoryLimitKb, stackLimitKb);
            executeMessages.add(result);
            maxTime = Math.max(maxTime, result.getTime() == null ? 0L : result.getTime());
            maxMemory = Math.max(maxMemory, result.getMemory() == null ? 0L : result.getMemory());

            if (StrUtil.isNotBlank(result.getErrorMessage())) {
                break;
            }
        }

        List<String> outputList = new ArrayList<>();
        String errorMessage = null;
        String verdict = null;
        for (ExecuteMessage message : executeMessages) {
            if (StrUtil.isNotBlank(message.getErrorMessage())) {
                errorMessage = message.getErrorMessage();
                verdict = message.getVerdict();
                break;
            }
            outputList.add(StrUtil.nullToEmpty(message.getMessage()));
        }

        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);
        judgeInfo.setMessage(errorMessage == null ? "执行成功" : errorMessage);

        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setOutputList(outputList);
        response.setMessage(errorMessage);
        response.setStatus(errorMessage == null ? "1" : "3");
        response.setVerdict(verdict);
        response.setJudgeInfo(judgeInfo);
        return response;
    }

    private ExecuteMessage runOneTestCase(
            DockerClient dockerClient, String image, String codeVolumeName, String input,
            long timeoutMillis, long memoryLimitKb, long stackLimitKb) throws Exception {
        long heapLimitKb = Math.max(16L * 1024,
                Math.min(256L * 1024, memoryLimitKb / 2));
        // Only Base64 and validated numeric limits enter the shell, never raw problem input.
        String encoded = java.util.Base64.getEncoder().encodeToString(
                StrUtil.nullToEmpty(input).getBytes(StandardCharsets.UTF_8));
        String[] command = new String[]{"sh", "-c", "printf '%s' '" + encoded
                + "' | base64 -d | java -Xmx" + heapLimitKb + "k -Xss" + stackLimitKb
                + "k -Dfile.encoding=UTF-8 -cp " + CONTAINER_CODE_DIR + " Main"};

        String runtimeContainerId = null;
        try {
            runtimeContainerId = createIdleContainer(
                    dockerClient, image, codeVolumeName, AccessMode.ro,
                    memoryLimitKb * 1024, 64L, 64, SANDBOX_USER);
            return executeContainerCommand(
                    dockerClient, runtimeContainerId, command, timeoutMillis, true);
        } finally {
            // 每个测试用例销毁独立容器，/tmp、进程和其他可变状态不会带到下一用例。
            removeContainerQuietly(dockerClient, runtimeContainerId);
        }
    }

    private ExecuteMessage executeContainerCommand(
            DockerClient dockerClient, String containerId, String[] command,
            long timeoutMillis, boolean collectStats) throws Exception {

        ExecCreateCmdResponse exec = dockerClient.execCreateCmd(containerId)
                .withCmd(command)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .exec();

        final BoundedTextBuffer stdout = new BoundedTextBuffer(limits.getMaxOutputBytes());
        final BoundedTextBuffer stderr = new BoundedTextBuffer(limits.getMaxOutputBytes());
        final long[] peakMemory = {0L};
        final AtomicBoolean stopRequested = new AtomicBoolean(false);

        ResultCallback.Adapter<Statistics> statsCallback = new ResultCallback.Adapter<Statistics>() {
            @Override
            public void onNext(Statistics statistics) {
                if (statistics.getMemoryStats() != null
                        && statistics.getMemoryStats().getUsage() != null) {
                    peakMemory[0] = Math.max(
                            peakMemory[0], statistics.getMemoryStats().getUsage());
                }
            }
        };
        StatsCmd statsCmd = null;
        if (collectStats) {
            statsCmd = dockerClient.statsCmd(containerId);
            statsCmd.exec(statsCallback);
        }

        StopWatch stopWatch = new StopWatch();
        boolean completed;
        ExecStartResultCallback execCallback = new ExecStartResultCallback() {
            @Override
            public void onNext(Frame frame) {
                boolean outputExceeded;
                if (StreamType.STDERR.equals(frame.getStreamType())) {
                    outputExceeded = stderr.append(frame.getPayload());
                } else {
                    outputExceeded = stdout.append(frame.getPayload());
                }
                if (outputExceeded && stopRequested.compareAndSet(false, true)) {
                    stopContainerAsync(dockerClient, containerId);
                }
                super.onNext(frame);
            }
        };

        try {
            stopWatch.start();
            completed = dockerClient.execStartCmd(exec.getId())
                    .exec(execCallback)
                    .awaitCompletion(timeoutMillis, TimeUnit.MILLISECONDS);
            stopWatch.stop();
        } finally {
            if (statsCmd != null) {
                statsCmd.close();
                statsCallback.close();
            }
            execCallback.close();
        }

        ExecuteMessage result = new ExecuteMessage();
        result.setTime(stopWatch.getLastTaskTimeMillis());
        result.setMemory((peakMemory[0] + 1023L) / 1024L);

        if (stdout.isTruncated() || stderr.isTruncated()) {
            result.setErrorMessage("程序输出超过限制（" + limits.getMaxOutputBytes() + " bytes）");
            result.setVerdict("OLE");
            return result;
        }

        if (!completed) {
            stopContainerQuietly(dockerClient, containerId);
            result.setErrorMessage("执行超时（限制 " + timeoutMillis + " ms）");
            result.setVerdict("TLE");
            return result;
        }

        InspectExecResponse execResult = dockerClient.inspectExecCmd(exec.getId()).exec();
        result.setExitValue(execResult.getExitCode());
        result.setMessage(stdout.getText());
        if (execResult.getExitCode() == null
                || execResult.getExitCode() != 0) {
            result.setErrorMessage(StrUtil.blankToDefault(
                    stderr.getText(), "程序异常退出，退出码：" + execResult.getExitCode()));
            result.setVerdict(stderr.getText().contains("OutOfMemoryError") ? "MLE" : "RE");
            if (Integer.valueOf(137).equals(execResult.getExitCode())) {
                result.setErrorMessage("内存超限或进程被系统终止：" + stderr.getText());
                result.setVerdict("MLE");
            }
        }
        return result;
    }

    void validateRequest(ExecuteCodeRequest request) {
        if (request == null || StrUtil.isBlank(request.getCode())) {
            throw new IllegalArgumentException("代码不能为空");
        }
        if (utf8Length(request.getCode()) > limits.getMaxCodeBytes()) {
            throw new IllegalArgumentException(
                    "代码超过大小限制（" + limits.getMaxCodeBytes() + " bytes）");
        }
        if (StrUtil.isBlank(request.getLanguage())
                || !"java".equals(request.getLanguage().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("当前仅支持 Java");
        }
        if (request.getInputList() == null || request.getInputList().isEmpty()) {
            request.setInputList(Collections.singletonList(""));
        }
        if (request.getInputList().size() > limits.getMaxTestCases()) {
            throw new IllegalArgumentException(
                    "测试用例数量超过限制（" + limits.getMaxTestCases() + "）");
        }
        int totalInputBytes = 0;
        for (String input : request.getInputList()) {
            int inputBytes = utf8Length(StrUtil.nullToEmpty(input));
            if (inputBytes > limits.getMaxInputBytesPerCase()) {
                throw new IllegalArgumentException("单个测试用例输入超过大小限制");
            }
            totalInputBytes += inputBytes;
            if (totalInputBytes > limits.getMaxTotalInputBytes()) {
                throw new IllegalArgumentException("测试用例输入总量超过大小限制");
            }
        }
    }

    private ExecuteCodeResponse userCodeErrorResponse(String message, Long time) {
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(message);
        judgeInfo.setTime(time == null ? 0L : time);
        judgeInfo.setMemory(0L);

        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setOutputList(new ArrayList<>());
        response.setMessage(message);
        response.setStatus("3");
        response.setJudgeInfo(judgeInfo);
        return response;
    }

    private boolean acquireExecutionSlot() {
        try {
            boolean acquired = executionSlots.tryAcquire(
                    Math.max(0L, limits.getQueueWaitTimeoutMs()), TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new IllegalStateException("沙箱并发任务已满，请稍后重试");
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待沙箱执行资源时被中断", e);
        }
    }

    long limitedOrDefault(
            String resourceName, Long value, long defaultValue, long min, long max) {
        long resolved = value == null || value <= 0 ? defaultValue : value;
        if (resolved < min || resolved > max) {
            throw new IllegalArgumentException(resourceName + "限制必须在 " + min + " 到 " + max + " 之间");
        }
        return resolved;
    }

    private int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private ExecuteCodeResponse getErrorResponse(Throwable e, String requestId) {
        log.error("代码沙箱执行失败, requestId: {}", requestId, e);
        String publicMessage = infrastructureErrorMessage(e);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(publicMessage);
        judgeInfo.setTime(0L);
        judgeInfo.setMemory(0L);

        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setOutputList(new ArrayList<>());
        response.setMessage(publicMessage);
        response.setStatus("2");
        response.setJudgeInfo(judgeInfo);
        return response;
    }

    /** 只向调用方暴露可操作的基础设施提示，不泄露堆栈或主机信息。 */
    String infrastructureErrorMessage(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof ConnectException) {
                return "Docker Engine 无法连接，请运行 scripts/start-sandbox-vm.ps1 建立 2375 SSH 隧道";
            }
            String message = current.getMessage();
            if (message != null && message.startsWith("Docker 镜像 ")) {
                return message;
            }
            if (message != null && message.startsWith("沙箱并发任务已满")) {
                return message;
            }
        }
        return "代码沙箱内部错误";
    }

    private void stopContainerQuietly(DockerClient dockerClient, String containerId) {
        if (dockerClient == null || containerId == null) {
            return;
        }
        try {
            dockerClient.stopContainerCmd(containerId).withTimeout(0).exec();
        } catch (Exception ignored) {
            // finally 中仍会强制删除容器。
        }
    }

    private void stopContainerAsync(DockerClient dockerClient, String containerId) {
        Thread stopper = new Thread(
                () -> stopContainerQuietly(dockerClient, containerId),
                "sandbox-output-limit-stop");
        stopper.setDaemon(true);
        stopper.start();
    }

    private void removeContainerQuietly(DockerClient dockerClient, String containerId) {
        if (dockerClient == null || containerId == null) {
            return;
        }
        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .withRemoveVolumes(true)
                    .exec();
        } catch (Exception ignored) {
            // 清理失败不能覆盖本次执行结果。
        }
    }

    private void closeDockerClientQuietly(DockerClient dockerClient) {
        if (dockerClient == null) {
            return;
        }
        try {
            dockerClient.close();
        } catch (Exception ignored) {
            // 无需处理。
        }
    }

    private void removeVolumeQuietly(DockerClient dockerClient, String volumeName) {
        if (dockerClient == null || volumeName == null) {
            return;
        }
        try {
            dockerClient.removeVolumeCmd(volumeName).exec();
        } catch (Exception ignored) {
            // 清理失败不能覆盖本次执行结果。
        }
    }

    private static final class BoundedTextBuffer {

        private final int maxBytes;

        private final java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();

        private boolean truncated;

        private BoundedTextBuffer(int maxBytes) {
            this.maxBytes = Math.max(1, maxBytes);
        }

        private synchronized boolean append(byte[] bytes) {
            int remaining = maxBytes - output.size();
            if (remaining > 0) {
                output.write(bytes, 0, Math.min(remaining, bytes.length));
            }
            if (bytes.length > remaining) {
                truncated = true;
            }
            return truncated;
        }

        private synchronized String getText() {
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }

        private synchronized boolean isTruncated() {
            return truncated;
        }
    }
}
