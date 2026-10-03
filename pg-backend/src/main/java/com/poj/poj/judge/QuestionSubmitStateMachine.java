package com.poj.poj.judge;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.poj.poj.judge.codesandbox.model.JudgeInfo;
import com.poj.poj.mapper.QuestionMapper;
import com.poj.poj.mapper.QuestionSubmitMapper;
import com.poj.poj.model.entity.QuestionSubmit;
import com.poj.poj.model.enums.QuestionSubmitStatusEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;

@Component
public class QuestionSubmitStateMachine {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final QuestionSubmitMapper questionSubmitMapper;
    private final QuestionMapper questionMapper;

    public QuestionSubmitStateMachine(QuestionSubmitMapper questionSubmitMapper,
                                      QuestionMapper questionMapper) {
        this.questionSubmitMapper = questionSubmitMapper;
        this.questionMapper = questionMapper;
    }

    /** 原子抢占待判题任务；重复投递只有一个工作线程能成功。 */
    public boolean tryStart(long submissionId, String token, Date deadline) {
        LambdaUpdateWrapper<QuestionSubmit> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(QuestionSubmit::getId, submissionId)
                .eq(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.WAITING.getValue())
                .eq(QuestionSubmit::getIsDelete, 0)
                .and(condition -> condition.isNull(QuestionSubmit::getNextRetryTime)
                        .or().le(QuestionSubmit::getNextRetryTime, new Date()))
                .set(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.RUNNING.getValue())
                .set(QuestionSubmit::getJudgeToken, token)
                .set(QuestionSubmit::getJudgeDeadline, deadline)
                .set(QuestionSubmit::getNextRetryTime, null)
                .set(QuestionSubmit::getLastError, null)
                .setSql("judgeAttempt = judgeAttempt + 1");
        return questionSubmitMapper.update(null, wrapper) == 1;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean complete(long submissionId, String token, long questionId,
                            JudgeInfo judgeInfo, boolean accepted) {
        boolean updated = finish(submissionId, token, QuestionSubmitStatusEnum.SUCCEED, judgeInfo, null);
        if (updated && accepted && questionMapper.incrementAcceptedNum(questionId) != 1) {
            throw new IllegalStateException("题目通过数更新失败");
        }
        return updated;
    }

    /** 系统故障按次数退避重试；达到上限后才进入失败终态。 */
    public boolean retryOrFail(long submissionId, String token, JudgeInfo judgeInfo,
                               int maxAttempts, long retryDelayMs, Throwable cause) {
        QuestionSubmit current = questionSubmitMapper.selectById(submissionId);
        if (current == null
                || !Objects.equals(current.getStatus(), QuestionSubmitStatusEnum.RUNNING.getValue())
                || !Objects.equals(current.getJudgeToken(), token)) {
            return false;
        }
        String error = rootMessage(cause);
        int attempts = current.getJudgeAttempt() == null ? 1 : current.getJudgeAttempt();
        if (attempts >= maxAttempts) {
            judgeInfo.setDetail("判题服务异常，已达到最大重试次数：" + error);
            return finish(submissionId, token, QuestionSubmitStatusEnum.FAILED, judgeInfo, error);
        }

        LambdaUpdateWrapper<QuestionSubmit> wrapper = ownedRunningTask(submissionId, token);
        wrapper.set(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.WAITING.getValue())
                .set(QuestionSubmit::getJudgeInfo, JSONUtil.toJsonStr(judgeInfo))
                .set(QuestionSubmit::getJudgeToken, null)
                .set(QuestionSubmit::getJudgeDeadline, null)
                .set(QuestionSubmit::getNextRetryTime, new Date(System.currentTimeMillis() + retryDelayMs))
                .set(QuestionSubmit::getLastError, error);
        return questionSubmitMapper.update(null, wrapper) == 1;
    }

    /** 回收进程崩溃或永久阻塞造成的过期 RUNNING 任务。 */
    public int recoverExpired(Date now, int maxAttempts, JudgeInfo timeoutInfo) {
        LambdaUpdateWrapper<QuestionSubmit> exhaustedWaiting = new LambdaUpdateWrapper<>();
        exhaustedWaiting.eq(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.WAITING.getValue())
                .eq(QuestionSubmit::getIsDelete, 0)
                .ge(QuestionSubmit::getJudgeAttempt, maxAttempts)
                .set(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.FAILED.getValue())
                .set(QuestionSubmit::getJudgeInfo, JSONUtil.toJsonStr(timeoutInfo))
                .set(QuestionSubmit::getNextRetryTime, null)
                .set(QuestionSubmit::getLastError, "判题任务已达到最大重试次数");
        questionSubmitMapper.update(null, exhaustedWaiting);

        LambdaUpdateWrapper<QuestionSubmit> failWrapper = expiredRunning(now);
        failWrapper.ge(QuestionSubmit::getJudgeAttempt, maxAttempts)
                .set(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.FAILED.getValue())
                .set(QuestionSubmit::getJudgeInfo, JSONUtil.toJsonStr(timeoutInfo))
                .set(QuestionSubmit::getJudgeToken, null)
                .set(QuestionSubmit::getJudgeDeadline, null)
                .set(QuestionSubmit::getLastError, "判题任务超时且已达到最大重试次数");
        questionSubmitMapper.update(null, failWrapper);

        LambdaUpdateWrapper<QuestionSubmit> retryWrapper = expiredRunning(now);
        retryWrapper.lt(QuestionSubmit::getJudgeAttempt, maxAttempts)
                .set(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.WAITING.getValue())
                .set(QuestionSubmit::getJudgeToken, null)
                .set(QuestionSubmit::getJudgeDeadline, null)
                .set(QuestionSubmit::getNextRetryTime, now)
                .set(QuestionSubmit::getLastError, "判题任务租约超时，已重新入队");
        return questionSubmitMapper.update(null, retryWrapper);
    }

    private boolean finish(long submissionId, String token, QuestionSubmitStatusEnum target,
                           JudgeInfo judgeInfo, String error) {
        LambdaUpdateWrapper<QuestionSubmit> wrapper = ownedRunningTask(submissionId, token);
        wrapper.set(QuestionSubmit::getStatus, target.getValue())
                .set(QuestionSubmit::getJudgeInfo, JSONUtil.toJsonStr(judgeInfo))
                .set(QuestionSubmit::getJudgeToken, null)
                .set(QuestionSubmit::getJudgeDeadline, null)
                .set(QuestionSubmit::getNextRetryTime, null)
                .set(QuestionSubmit::getLastError, error);
        return questionSubmitMapper.update(null, wrapper) == 1;
    }

    private LambdaUpdateWrapper<QuestionSubmit> ownedRunningTask(long submissionId, String token) {
        LambdaUpdateWrapper<QuestionSubmit> wrapper = new LambdaUpdateWrapper<>();
        return wrapper.eq(QuestionSubmit::getId, submissionId)
                .eq(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.RUNNING.getValue())
                .eq(QuestionSubmit::getJudgeToken, token)
                .eq(QuestionSubmit::getIsDelete, 0);
    }

    private LambdaUpdateWrapper<QuestionSubmit> expiredRunning(Date now) {
        LambdaUpdateWrapper<QuestionSubmit> wrapper = new LambdaUpdateWrapper<>();
        return wrapper.eq(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.RUNNING.getValue())
                .eq(QuestionSubmit::getIsDelete, 0)
                .and(condition -> condition.isNull(QuestionSubmit::getJudgeDeadline)
                        .or().le(QuestionSubmit::getJudgeDeadline, now));
    }

    private String rootMessage(Throwable cause) {
        Throwable current = cause;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        String message = current == null ? "未知判题异常" : current.getMessage();
        if (StringUtils.isBlank(message)) {
            message = current == null ? "未知判题异常" : current.getClass().getSimpleName();
        }
        return StringUtils.abbreviate(message, MAX_ERROR_LENGTH);
    }
}
