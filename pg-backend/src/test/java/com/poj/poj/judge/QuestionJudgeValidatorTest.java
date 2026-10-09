package com.poj.poj.judge;

import com.poj.poj.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuestionJudgeValidatorTest {
    private final String valid = "{\"timeLimit\":1000,\"memoryLimit\":262144,\"stackLimit\":1024}";
    @Test void rejectsMissingEmptyAndMalformedCases() {
        for (String input : new String[]{null, "[]", "{}", "[{\"input\":\"1 2\"}]", "not-json"}) {
            assertThrows(BusinessException.class, () -> QuestionJudgeValidator.validate(input, valid));
        }
    }
    @Test void acceptsEmptyStdinAndOutputButRejectsUnsafeLimits() {
        assertDoesNotThrow(() -> QuestionJudgeValidator.validate("[{\"input\":\"\",\"output\":\"\"}]", valid));
        assertThrows(BusinessException.class, () -> QuestionJudgeValidator.validateConfig("{\"timeLimit\":1000,\"memoryLimit\":1000,\"stackLimit\":1000}"));
        assertThrows(BusinessException.class, () -> QuestionJudgeValidator.validateConfig("{\"timeLimit\":0,\"memoryLimit\":262144,\"stackLimit\":1024}"));
    }
}
