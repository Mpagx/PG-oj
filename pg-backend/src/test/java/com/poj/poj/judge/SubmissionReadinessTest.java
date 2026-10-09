package com.poj.poj.judge;

import com.poj.poj.exception.BusinessException;
import com.poj.poj.judge.codesandbox.impl.RemoteCodeSandbox;
import com.poj.poj.judge.codesandbox.model.SandboxHealthResponse;
import com.poj.poj.mapper.QuestionSubmitMapper;
import com.poj.poj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.poj.poj.model.entity.Question;
import com.poj.poj.model.entity.User;
import com.poj.poj.service.QuestionService;
import com.poj.poj.service.impl.QuestionSubmitServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubmissionReadinessTest {
    @Test void offlineDockerDoesNotCreateSubmissionOrQueueEvent() {
        QuestionSubmitServiceImpl service = new QuestionSubmitServiceImpl();
        QuestionService questions = mock(QuestionService.class);
        RemoteCodeSandbox sandbox = mock(RemoteCodeSandbox.class);
        QuestionSubmitMapper submissions = mock(QuestionSubmitMapper.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        ReflectionTestUtils.setField(service, "questionService", questions);
        ReflectionTestUtils.setField(service, "remoteCodeSandbox", sandbox);
        ReflectionTestUtils.setField(service, "sandboxType", "remote");
        ReflectionTestUtils.setField(service, "baseMapper", submissions);
        ReflectionTestUtils.setField(service, "eventPublisher", events);
        Question problem = new Question(); problem.setId(15L); problem.setStatus("PUBLISHED");
        problem.setJudgeCase("[{\"input\":\"1 2\",\"output\":\"3\"}]");
        problem.setJudgeConfig("{\"timeLimit\":1000,\"memoryLimit\":262144,\"stackLimit\":1024}");
        when(questions.getById(15L)).thenReturn(problem);
        when(sandbox.health()).thenReturn(SandboxHealthResponse.down("Docker unavailable"));
        QuestionSubmitAddRequest request = new QuestionSubmitAddRequest();
        request.setQuestionId(15L); request.setLanguage("java"); request.setCode("public class Main { public static void main(String[] args) {} }");
        User user = new User(); user.setId(1L);
        BusinessException error = assertThrows(BusinessException.class, () -> service.doQuestionSubmit(request, user));
        assertTrue(error.getMessage().contains("未创建提交"));
        verifyNoInteractions(submissions, events);
    }

    @Test void draftQuestionCannotBeSubmitted() {
        QuestionSubmitServiceImpl service = new QuestionSubmitServiceImpl();
        QuestionService questions = mock(QuestionService.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        ReflectionTestUtils.setField(service, "questionService", questions);
        ReflectionTestUtils.setField(service, "eventPublisher", events);

        Question problem = new Question();
        problem.setId(16L);
        problem.setStatus("DRAFT");
        when(questions.getById(16L)).thenReturn(problem);

        QuestionSubmitAddRequest request = new QuestionSubmitAddRequest();
        request.setQuestionId(16L);
        request.setLanguage("java");
        request.setCode("public class Main { public static void main(String[] args) {} }");
        User user = new User(); user.setId(1L);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.doQuestionSubmit(request, user));
        assertTrue(error.getMessage().contains("尚未发布"));
        verifyNoInteractions(events);
    }
}
