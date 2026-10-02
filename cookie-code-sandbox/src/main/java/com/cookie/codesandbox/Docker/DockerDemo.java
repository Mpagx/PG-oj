package com.cookie.codesandbox.Docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import javax.xml.parsers.DocumentBuilder;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class DockerDemo {

    private static final String DEFAULT_DOCKER_HOST = "tcp://127.0.0.1:2375";

    public static void main(String[] args) throws InterruptedException, IOException {
        String dockerHost = resolveDockerHost();
        DockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();

        // Apache HttpClient 5 transport 支持通过 TCP 连接 Linux 虚拟机中的 Docker Engine。
        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        try (DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient)) {
            dockerClient.pingCmd().exec();
            System.out.println("Docker 连接成功: " + dockerHost);

            String image = "nginx:latest";
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像: " + item.getStatus());
                    super.onNext(item);
                }
            };
            pullImageCmd
                    .exec(pullImageResultCallback)
                    .awaitCompletion();
            System.out.println("下载完成");

            // --- 新增：创建容器 ---
            CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
            CreateContainerResponse createContainerResponse = containerCmd
                    .withCmd("echo", "Hello Docker")
                    .exec();
            System.out.println(createContainerResponse);
            // --- 新增结束 ---
            String containerId = createContainerResponse.getId();

// 查看容器状态
            ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
            List<Container> containerList = listContainersCmd.withShowAll(true).exec();
            for (Container container : containerList) {
                System.out.println(container);
            }

// 启动容器
            dockerClient.startContainerCmd(containerId).exec();
        }
    }

    private static String resolveDockerHost() {
        String dockerHost = System.getProperty("docker.host");
        if (dockerHost == null || dockerHost.trim().isEmpty()) {
            dockerHost = System.getenv("DOCKER_HOST");
        }
        return dockerHost == null || dockerHost.trim().isEmpty()
                ? DEFAULT_DOCKER_HOST
                : dockerHost.trim();
    }
}
