package com.poj.poj.judge;

import com.poj.poj.judge.codesandbox.model.JudgeInfo;
import com.poj.poj.model.enums.JudgeInfoMessageEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class QuestionSubmitJudgeListener {

    private final JudgeService judgeService;

    private final QuestionSubmitStateMachine stateMachine;

    public QuestionSubmitJudgeListener(JudgeService judgeService,
                                       QuestionSubmitStateMachine stateMachine) {
        this.judgeService = judgeService;
        this.stateMachine = stateMachine;
    }

    @Async("judgeExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(QuestionSubmitCreatedEvent event) {
        long submissionId = event.getQuestionSubmitId();
        try {
            judgeService.doJudge(submissionId);
        } catch (RuntimeException e) {
            JudgeInfo failure = new JudgeInfo();
            failure.setMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getValue());
            failure.setDetail(e.getMessage());
            stateMachine.fail(submissionId, failure);
            log.error("异步判题任务执行异常, questionSubmitId: {}", submissionId, e);
        }
    }
}
