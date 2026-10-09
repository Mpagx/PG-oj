package com.poj.poj.judge;

import com.poj.poj.judge.codesandbox.impl.RemoteCodeSandbox;
import com.poj.poj.judge.codesandbox.model.ExecuteCodeResponse;
import com.poj.poj.judge.codesandbox.model.JudgeInfo;
import com.poj.poj.judge.codesandbox.model.SandboxHealthResponse;
import com.poj.poj.model.dto.questionsubmit.CustomTestRequest;
import com.poj.poj.model.entity.Question;
import com.poj.poj.model.entity.User;
import com.poj.poj.model.vo.CustomTestResultVO;
import com.poj.poj.service.QuestionService;
import com.poj.poj.service.impl.QuestionSubmitServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomTestServiceTest {
    @Test
    void executesOneInputWithoutCreatingSubmission() {
        QuestionSubmitServiceImpl service = new QuestionSubmitServiceImpl();
        QuestionService questions = mock(QuestionService.class);
        RemoteCodeSandbox sandbox = mock(RemoteCodeSandbox.class);
        ReflectionTestUtils.setField(service, "questionService", questions);
        ReflectionTestUtils.setField(service, "remoteCodeSandbox", sandbox);
        ReflectionTestUtils.setField(service, "sandboxType", "remote");

        Question question = new Question();
        question.setId(15L);
        question.setStatus("PUBLISHED");
        question.setJudgeConfig("{\"timeLimit\":1000,\"memoryLimit\":262144,\"stackLimit\":1024}");
        when(questions.getById(15L)).thenReturn(question);
        when(sandbox.health()).thenReturn(new SandboxHealthResponse(
                "UP", "cookie-code-sandbox", "1.0.0", List.of("java"), "ready"));
        JudgeInfo info = new JudgeInfo(); info.setTime(12L); info.setMemory(1024L);
        ExecuteCodeResponse response = ExecuteCodeResponse.builder()
                .protocolVersion("1.0").status("1").outputList(List.of("3\n")).judgeInfo(info).build();
        when(sandbox.executeCode(any())).thenReturn(response);

        CustomTestRequest request = new CustomTestRequest();
        request.setQuestionId(15L); request.setLanguage("java"); request.setInput("1 2");
        request.setCode("public class Main { public static void main(String[] args) { System.out.println(3); } }");
        User user = new User(); user.setId(7L);
        CustomTestResultVO result = service.runCustomTest(request, user);

        assertEquals("3\n", result.getOutput());
        assertEquals(12L, result.getTime());
        verify(sandbox).executeCode(argThat(value -> value.getInputList().equals(List.of("1 2"))));
    }
}
