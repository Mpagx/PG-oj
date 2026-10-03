package com.poj.poj.judge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.poj.poj.judge.codesandbox.model.JudgeInfo;
import com.poj.poj.mapper.QuestionSubmitMapper;
import com.poj.poj.model.entity.QuestionSubmit;
import com.poj.poj.model.enums.JudgeInfoMessageEnum;
import com.poj.poj.model.enums.QuestionSubmitStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 数据库持久化 + 有界内存执行器组成的判题队列。 */
@Component
@Slf4j
public class JudgeTaskQueue {

    private final JudgeService judgeService;
    private final QuestionSubmitMapper questionSubmitMapper;
    private final QuestionSubmitStateMachine stateMachine;
    private final ThreadPoolTaskExecutor executor;
    private final Set<Long> queuedIds = ConcurrentHashMap.newKeySet();
    private final int scanBatchSize;
    private final int maxAttempts;
    private final boolean recoveryEnabled;

    public JudgeTaskQueue(JudgeService judgeService,
                          QuestionSubmitMapper questionSubmitMapper,
                          QuestionSubmitStateMachine stateMachine,
                          @Qualifier("judgeExecutor") ThreadPoolTaskExecutor executor,
                          @Value("${judge.queue.scan-batch-size:50}") int scanBatchSize,
                          @Value("${judge.max-attempts:3}") int maxAttempts,
                          @Value("${judge.queue.recovery-enabled:true}") boolean recoveryEnabled) {
        this.judgeService = judgeService;
        this.questionSubmitMapper = questionSubmitMapper;
        this.stateMachine = stateMachine;
        this.executor = executor;
        this.scanBatchSize = Math.max(1, scanBatchSize);
        this.maxAttempts = Math.max(1, maxAttempts);
        this.recoveryEnabled = recoveryEnabled;
    }

    /** 重复事件在本实例内只排队一次；跨实例重复由数据库原子抢占兜底。 */
    public boolean enqueue(long submissionId) {
        if (!queuedIds.add(submissionId)) {
            return false;
        }
        try {
            executor.execute(() -> runTask(submissionId));
            return true;
        } catch (TaskRejectedException e) {
            queuedIds.remove(submissionId);
            log.warn("判题内存队列已满，任务保留在数据库等待重新投递, questionSubmitId: {}", submissionId);
            return false;
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        recoverAndDispatch();
    }

    @Scheduled(fixedDelayString = "${judge.queue.scan-interval-ms:5000}",
            initialDelayString = "${judge.queue.scan-interval-ms:5000}")
    public void recoverAndDispatch() {
        if (!recoveryEnabled) {
            return;
        }
        JudgeInfo timeoutInfo = new JudgeInfo();
        timeoutInfo.setMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getValue());
        timeoutInfo.setDetail("判题任务执行超时");
        int recovered = stateMachine.recoverExpired(new Date(), maxAttempts, timeoutInfo);
        if (recovered > 0) {
            log.warn("已恢复 {} 个超时判题任务", recovered);
        }

        Date now = new Date();
        LambdaQueryWrapper<QuestionSubmit> query = new LambdaQueryWrapper<>();
        query.select(QuestionSubmit::getId)
                .eq(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.WAITING.getValue())
                .eq(QuestionSubmit::getIsDelete, 0)
                .lt(QuestionSubmit::getJudgeAttempt, maxAttempts)
                .and(condition -> condition.isNull(QuestionSubmit::getNextRetryTime)
                        .or().le(QuestionSubmit::getNextRetryTime, now))
                .orderByAsc(QuestionSubmit::getId)
                .last("LIMIT " + scanBatchSize);
        List<QuestionSubmit> waiting = questionSubmitMapper.selectList(query);
        waiting.forEach(item -> enqueue(item.getId()));
    }

    private void runTask(long submissionId) {
        try {
            judgeService.doJudge(submissionId);
        } catch (RuntimeException e) {
            log.error("判题工作线程异常, questionSubmitId: {}", submissionId, e);
        } finally {
            queuedIds.remove(submissionId);
        }
    }
}
