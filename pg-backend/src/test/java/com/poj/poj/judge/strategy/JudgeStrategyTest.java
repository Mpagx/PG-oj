package com.poj.poj.judge.strategy;

import com.poj.poj.judge.codesandbox.model.JudgeInfo;
import com.poj.poj.model.dto.question.JudgeCase;
import com.poj.poj.model.entity.Question;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class JudgeStrategyTest {
    @Test void normalizesTrailingWhitespaceWithoutChangingLeadingOrInternalSpaces() {
        assertEquals("3", DefaultJudgeStrategy.normalize("3 \t\r\n\r\n"));
        assertEquals(" a\n\nb", DefaultJudgeStrategy.normalize(" a  \r\n\r\nb\n"));
        assertNotEquals(DefaultJudgeStrategy.normalize("1 2"), DefaultJudgeStrategy.normalize("12"));
    }
    @Test void reportsActualPassedCasesAndEnforcesJavaTimeLimit() {
        JudgeContext context = new JudgeContext();
        Question question = new Question();
        question.setJudgeConfig("{\"timeLimit\":1000,\"memoryLimit\":262144,\"stackLimit\":1024}");
        context.setQuestion(question);
        JudgeCase first = new JudgeCase(); first.setInput("1 2"); first.setOutput("3");
        JudgeCase second = new JudgeCase(); second.setInput("3 4"); second.setOutput("7");
        context.setJudgeCaseList(Arrays.asList(first, second));
        context.setOutputList(Arrays.asList("3\n", "8"));
        JudgeInfo execution = new JudgeInfo(); execution.setMemory(32000L); execution.setTime(40L);
        context.setJudgeInfo(execution);
        JudgeInfo result = new JavaLanguageJudgeStrategy().doJudge(context);
        assertEquals("Wrong Answer", result.getMessage()); assertEquals(1, result.getPassedCaseCount());
        context.setOutputList(Arrays.asList("3\n", "7\n"));
        execution.setTime(1100L);
        assertEquals("超时", new JavaLanguageJudgeStrategy().doJudge(context).getMessage());
    }
}
