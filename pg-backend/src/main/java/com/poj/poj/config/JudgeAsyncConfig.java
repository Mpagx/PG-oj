package com.poj.poj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class JudgeAsyncConfig {

    @Bean("judgeExecutor")
    public ThreadPoolTaskExecutor judgeExecutor(
            @Value("${judge.queue.core-pool-size:2}") int corePoolSize,
            @Value("${judge.queue.max-pool-size:4}") int maxPoolSize,
            @Value("${judge.queue.capacity:100}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("judge-");
        // 队列满时拒绝本次内存投递；任务仍保留在数据库，稍后由扫描器重新投递。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
