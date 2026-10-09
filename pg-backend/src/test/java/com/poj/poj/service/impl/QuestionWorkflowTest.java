package com.poj.poj.service.impl;

import com.poj.poj.exception.BusinessException;
import com.poj.poj.model.entity.Question;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestionWorkflowTest {

    private final QuestionServiceImpl service = new QuestionServiceImpl();

    @Test
    void acceptsSupportedDifficultyAndPublicationStatus() {
        Question question = new Question();
        question.setDifficulty("EASY");
        question.setStatus("DRAFT");

        assertDoesNotThrow(() -> service.validQuestion(question, false));
    }

    @Test
    void rejectsUnknownDifficultyAndPublicationStatus() {
        Question invalidDifficulty = new Question();
        invalidDifficulty.setDifficulty("EXPERT");
        assertThrows(BusinessException.class,
                () -> service.validQuestion(invalidDifficulty, false));

        Question invalidStatus = new Question();
        invalidStatus.setStatus("ARCHIVED");
        assertThrows(BusinessException.class,
                () -> service.validQuestion(invalidStatus, false));
    }
}
