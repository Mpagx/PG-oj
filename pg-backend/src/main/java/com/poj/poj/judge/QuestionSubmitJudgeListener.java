package com.poj.poj.judge;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class QuestionSubmitJudgeListener {

    private final JudgeTaskQueue judgeTaskQueue;

    public QuestionSubmitJudgeListener(JudgeTaskQueue judgeTaskQueue) {
        this.judgeTaskQueue = judgeTaskQueue;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(QuestionSubmitCreatedEvent event) {
        judgeTaskQueue.enqueue(event.getQuestionSubmitId());
    }
}
