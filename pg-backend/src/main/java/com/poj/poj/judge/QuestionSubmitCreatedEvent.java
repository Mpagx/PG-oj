package com.poj.poj.judge;

public class QuestionSubmitCreatedEvent {

    private final long questionSubmitId;

    public QuestionSubmitCreatedEvent(long questionSubmitId) {
        this.questionSubmitId = questionSubmitId;
    }

    public long getQuestionSubmitId() {
        return questionSubmitId;
    }
}
