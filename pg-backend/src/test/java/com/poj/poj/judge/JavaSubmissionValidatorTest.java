package com.poj.poj.judge;

import com.poj.poj.exception.BusinessException;
import com.poj.poj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.poj.poj.model.entity.User;
import com.poj.poj.service.QuestionService;
import com.poj.poj.service.impl.QuestionSubmitServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JavaSubmissionValidatorTest {
    @Test void rejectsPlainTextAndMissingOrFakeEntryPoints() {
        for (String code : new String[]{"111", "22", "hello", "public class Main {}",
                "class Main {public static void main(String[] args){}}",
                "public class Main {public void main(String[] args){}}",
                "public class Main {public static void main(int n){}}",
                "// public class Main { public static void main(String[] args) {} }",
                "public class Main {String s=\"public static void main(String[] args) {}\";}",
                "package demo; public class Main {public static void main(String[] args){}}",
                "class Outer {public static class Main {public static void main(String[] args){}}}"}) {
            assertThrows(BusinessException.class, () -> JavaSubmissionValidator.validate(code), code);
        }
    }
    @Test void acceptsNormalSignaturesButLeavesCompilationToSandbox() {
        for (String code : new String[]{
                "public class Main {public static void main(String[] args){System.out.println(3);}}",
                "public final class Main {static public void main(String args[]){}}",
                "public class Main {public static void main(String... args){}}",
                "public class Main {public static void main(java.lang.String[] args){}}",
                "public class Main {public static void main(String[] args){int n=\"wrong\";}}"}) {
            assertDoesNotThrow(() -> JavaSubmissionValidator.validate(code), code);
        }
    }
    @Test void invalidSourceIsRejectedBeforeDatabaseLookup() {
        QuestionSubmitServiceImpl service = new QuestionSubmitServiceImpl();
        QuestionService questions = mock(QuestionService.class);
        ReflectionTestUtils.setField(service, "questionService", questions);
        QuestionSubmitAddRequest request = new QuestionSubmitAddRequest();
        request.setQuestionId(15L); request.setLanguage("java"); request.setCode("111");
        BusinessException error = assertThrows(BusinessException.class, () -> service.doQuestionSubmit(request, new User()));
        assertEquals(40000, error.getCode());
        verifyNoInteractions(questions);
    }
}
