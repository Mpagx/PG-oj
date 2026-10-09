package com.poj.poj.judge;

import cn.hutool.json.JSONUtil;
import com.poj.poj.common.ErrorCode;
import com.poj.poj.exception.BusinessException;
import com.poj.poj.judge.codesandbox.CodeSandbox;
import com.poj.poj.judge.codesandbox.CodeSandboxFactory;
import com.poj.poj.judge.codesandbox.CodeSandboxProxy;
import com.poj.poj.judge.codesandbox.impl.RemoteCodeSandbox;
import com.poj.poj.judge.codesandbox.model.ExecuteCodeRequest;
import com.poj.poj.judge.codesandbox.model.ExecuteCodeResponse;
import com.poj.poj.judge.codesandbox.model.JudgeInfo;
import com.poj.poj.judge.strategy.JudgeContext;
import com.poj.poj.model.dto.question.JudgeCase;
import com.poj.poj.model.dto.question.JudgeConfig;
import com.poj.poj.model.entity.Question;
import com.poj.poj.model.entity.QuestionSubmit;
import com.poj.poj.model.enums.JudgeInfoMessageEnum;
import com.poj.poj.service.QuestionService;
import com.poj.poj.service.QuestionSubmitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Value("${codesandbox.type:example}")
    private String type;

    @Value("${judge.task-timeout-ms:45000}")
    private long taskTimeoutMs;

    @Value("${judge.max-attempts:3}")
    private int maxAttempts;

    @Value("${judge.retry-delay-ms:3000}")
    private long retryDelayMs;

    @Autowired
    private JudgeManager judgeManager;

    @Autowired
    private RemoteCodeSandbox remoteCodeSandbox;

    @Autowired
    private QuestionSubmitStateMachine stateMachine;
    @Resource
    private io.micrometer.core.instrument.MeterRegistry metrics;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }

        String token = UUID.randomUUID().toString();
        Date deadline = new Date(System.currentTimeMillis() + Math.max(1000L, taskTimeoutMs));
        if (!stateMachine.tryStart(questionSubmitId, token, deadline)) {
            // 重复事件、重复扫描或已经完成都视为幂等成功，不覆盖已有结果。
            return questionSubmitService.getById(questionSubmitId);
        }

        try {
            long started = System.nanoTime();
            QuestionSubmit result = executeJudge(questionSubmit, token);
            metrics.timer("poj.judge.duration").record(System.nanoTime() - started, java.util.concurrent.TimeUnit.NANOSECONDS);
            metrics.counter("poj.judge.attempts", "outcome", "completed").increment();
            return result;
        } catch (RuntimeException e) {
            metrics.counter("poj.judge.attempts", "outcome", "system_error").increment();
            JudgeInfo failure = new JudgeInfo();
            failure.setMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getValue());
            failure.setDetail("判题服务暂时不可用，正在自动重试");
            boolean recorded = stateMachine.retryOrFail(questionSubmitId, token, failure,
                    Math.max(1, maxAttempts), Math.max(0L, retryDelayMs), e);
            if (!recorded) {
                log.warn("判题失败状态未更新，任务租约可能已被回收, questionSubmitId: {}, token: {}",
                        questionSubmitId, token);
            }
            log.error("判题任务执行失败, questionSubmitId: {}, token: {}", questionSubmitId, token, e);
            return questionSubmitService.getById(questionSubmitId);
        }
    }

    private QuestionSubmit executeJudge(QuestionSubmit questionSubmit, String token) {
        long questionSubmitId = questionSubmit.getId();
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }

        CodeSandbox codeSandbox = "remote".equals(type)
                ? remoteCodeSandbox : CodeSandboxFactory.newInstance(type);
        codeSandbox = new CodeSandboxProxy(codeSandbox);

        List<JudgeCase> judgeCaseList = JSONUtil.toList(question.getJudgeCase(), JudgeCase.class);
        QuestionJudgeValidator.validate(question.getJudgeCase(), question.getJudgeConfig());
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        JudgeConfig judgeConfig = JSONUtil.toBean(question.getJudgeConfig(), JudgeConfig.class);
        if (judgeConfig == null || judgeConfig.getTimeLimit() == null
                || judgeConfig.getMemoryLimit() == null || judgeConfig.getStackLimit() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目判题限制配置无效");
        }

        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .protocolVersion("1.0")
                // 同一提交的重试使用稳定 requestId，便于沙箱侧追踪和去重。
                .requestId("question-submit-" + questionSubmitId)
                .code(questionSubmit.getCode())
                .language(questionSubmit.getLanguage())
                .inputList(inputList)
                .timeLimitMs(judgeConfig.getTimeLimit())
                .memoryLimitKb(judgeConfig.getMemoryLimit())
                .stackLimitKb(judgeConfig.getStackLimit())
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        if ("2".equals(executeCodeResponse.getStatus())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "代码沙箱执行异常：" + executeCodeResponse.getMessage());
        }

        List<String> outputList = executeCodeResponse.getOutputList();
        if ("3".equals(executeCodeResponse.getStatus())) {
            JudgeInfo userCodeFailure = executeCodeResponse.getJudgeInfo();
            if (userCodeFailure == null) {
                userCodeFailure = new JudgeInfo();
            }
            String detail = executeCodeResponse.getMessage();
            if (detail == null && userCodeFailure.getMessage() != null) {
                detail = userCodeFailure.getMessage();
            }
            String verdict = executeCodeResponse.getVerdict();
            if ("CE".equals(verdict)) {
                userCodeFailure.setMessage(JudgeInfoMessageEnum.COMPILE_ERROR.getValue());
            } else if ("TLE".equals(verdict)) {
                userCodeFailure.setMessage(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getValue());
            } else if ("MLE".equals(verdict)) {
                userCodeFailure.setMessage(JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED.getValue());
            } else if ("OLE".equals(verdict)) {
                userCodeFailure.setMessage(JudgeInfoMessageEnum.OUTPUT_LIMIT_EXCEEDED.getValue());
            } else {
                userCodeFailure.setMessage(JudgeInfoMessageEnum.RUNTIME_ERROR.getValue());
            }
            int passed = 0;
            if (outputList != null) for (int i = 0; i < Math.min(outputList.size(), judgeCaseList.size()); i++) {
                if (com.poj.poj.judge.strategy.DefaultJudgeStrategy.normalize(outputList.get(i)).equals(
                        com.poj.poj.judge.strategy.DefaultJudgeStrategy.normalize(judgeCaseList.get(i).getOutput()))) passed++;
            }
            userCodeFailure.setPassedCaseCount(passed);
            userCodeFailure.setTotalCaseCount(judgeCaseList.size());
            userCodeFailure.setDetail(detail);
            if (!stateMachine.complete(questionSubmitId, token, questionId, userCodeFailure, false)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "判题任务租约已失效");
            }
            return questionSubmitService.getById(questionSubmitId);
        }

        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setInputList(inputList);
        judgeContext.setOutputList(outputList);
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setQuestion(question);
        judgeContext.setQuestionSubmit(questionSubmit);
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
        judgeInfo.setTotalCaseCount(judgeCaseList.size());
        judgeInfo.setDetail(executeCodeResponse.getMessage());
        boolean accepted = "Accepted".equals(judgeInfo.getMessage());
        if (!stateMachine.complete(questionSubmitId, token, questionId, judgeInfo, accepted)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "判题任务租约已失效");
        }
        return questionSubmitService.getById(questionSubmitId);
    }
}
