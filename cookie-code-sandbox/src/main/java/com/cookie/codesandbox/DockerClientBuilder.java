package com.cookie.codesandbox;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import java.time.Duration;
import java.net.InetAddress;
import java.net.URI;

/**
 * Docker 客户端构建器。
 * <p>
 * 基于 docker-java 3.3.3 的 ApacheDockerHttpClient 传输层，通过 TCP 连接 Docker 守护进程
 * （默认指向 Linux 虚拟机中的 Docker Engine）。
 */
public class DockerClientBuilder {

    private static final DockerClientBuilder INSTANCE = new DockerClientBuilder();

    private static final String DEFAULT_DOCKER_HOST = "tcp://127.0.0.1:2375";

    private DockerClientBuilder() {
    }

    public static DockerClientBuilder getInstance() {
        return INSTANCE;
    }

    public DockerClient build() {
        String dockerHost = System.getProperty("docker.host");
        if (dockerHost == null || dockerHost.trim().isEmpty()) {
            dockerHost = System.getenv("DOCKER_HOST");
        }
        if (dockerHost == null || dockerHost.trim().isEmpty()) {
            dockerHost = DEFAULT_DOCKER_HOST;
        }

        DefaultDockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();

        validateDockerEndpoint(config.getDockerHost(), config.getSSLConfig() != null);

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    /**
     * 明文 Docker TCP API 等同于宿主机 root 权限，只允许走本机回环地址。
     * 远程地址必须配置 Docker TLS（DOCKER_TLS_VERIFY + DOCKER_CERT_PATH）。
     */
    static void validateDockerEndpoint(URI dockerHost, boolean tlsEnabled) {
        if (dockerHost == null || !"tcp".equalsIgnoreCase(dockerHost.getScheme())) {
            return;
        }
        String host = dockerHost.getHost();
        if (tlsEnabled || isLoopback(host)) {
            return;
        }
        throw new IllegalStateException(
                "拒绝连接未启用 TLS 的远程 Docker API；请使用本机 SSH 隧道或配置 Docker TLS");
    }

    private static boolean isLoopback(String host) {
        if (host == null) {
            return false;
        }
        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (Exception ignored) {
            return false;
        }
    }
}
