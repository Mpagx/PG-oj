package com.poj.poj.controller;

import com.poj.poj.common.DeleteRequest;
import com.poj.poj.common.ErrorCode;
import com.poj.poj.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestionSolutionControllerTest {
    @Test
    void missingIdReturnsParameterErrorBeforeCallingServices() {
        QuestionSolutionController controller = new QuestionSolutionController();
        DeleteRequest emptyBody = new DeleteRequest();
        BusinessException reveal = assertThrows(BusinessException.class,
                () -> controller.reveal(emptyBody, null));
        BusinessException delete = assertThrows(BusinessException.class,
                () -> controller.delete(emptyBody, null));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), reveal.getCode());
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), delete.getCode());
    }
}
