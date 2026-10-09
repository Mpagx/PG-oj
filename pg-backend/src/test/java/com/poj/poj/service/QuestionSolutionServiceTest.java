package com.poj.poj.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.poj.poj.exception.BusinessException;
import com.poj.poj.mapper.QuestionSolutionMapper;
import com.poj.poj.model.dto.questionsolution.QuestionSolutionSaveRequest;
import com.poj.poj.model.entity.Question;
import com.poj.poj.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QuestionSolutionServiceTest {
    private QuestionSolutionService service;
    private QuestionService questions;
    private QuestionSubmitService submissions;
    private QuestionSolutionMapper mapper;

    @BeforeEach
    void setUp() {
        service = new QuestionSolutionService();
        questions = mock(QuestionService.class);
        submissions = mock(QuestionSubmitService.class);
        mapper = mock(QuestionSolutionMapper.class);
        ReflectionTestUtils.setField(service, "questionService", questions);
        ReflectionTestUtils.setField(service, "questionSubmitService", submissions);
        ReflectionTestUtils.setField(service, "userService", mock(UserService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void acceptedSubmissionUnlocksSolutions() {
        when(submissions.count(any(Wrapper.class))).thenReturn(1L, 0L);

        assertTrue(service.hasAccepted(9L, 7L));
        assertFalse(service.hasAccepted(9L, 8L));
    }

    @Test
    void userCannotPublishBeforeAccepted() {
        Question question = new Question();
        question.setId(9L);
        question.setStatus("PUBLISHED");
        when(questions.getById(9L)).thenReturn(question);
        when(submissions.count(any(Wrapper.class))).thenReturn(0L);
        User user = new User();
        user.setId(7L);
        QuestionSolutionSaveRequest request = new QuestionSolutionSaveRequest();
        request.setQuestionId(9L);
        request.setTitle("思路");
        request.setContent("正文");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.saveMine(request, user, false));
        assertTrue(error.getMessage().contains("通过这道题"));
    }

    @Test
    void userCanExplicitlyRevealBeforeAccepted() {
        Question question = new Question();
        question.setId(9L);
        question.setStatus("PUBLISHED");
        when(questions.getById(9L)).thenReturn(question);
        when(submissions.count(any(Wrapper.class))).thenReturn(0L);
        User user = new User();
        user.setId(7L);

        assertTrue(service.reveal(9L, user, false));
        verify(mapper).reveal(9L, 7L);
    }
}
