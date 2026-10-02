package com.cookie.codesandbox;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import java.time.Duration;

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

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }
}
