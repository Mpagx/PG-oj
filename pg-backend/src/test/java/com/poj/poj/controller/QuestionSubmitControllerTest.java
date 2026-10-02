package com.poj.poj.controller;

import com.poj.poj.exception.GlobalExceptionHandler;
import com.poj.poj.model.entity.QuestionSubmit;
import com.poj.poj.model.entity.User;
import com.poj.poj.model.vo.QuestionSubmitVO;
import com.poj.poj.service.QuestionSubmitService;
import com.poj.poj.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionSubmitControllerTest {
    private QuestionSubmitService questionSubmitService;
    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        questionSubmitService = mock(QuestionSubmitService.class);
        userService = mock(UserService.class);
        QuestionSubmitController controller = new QuestionSubmitController();
        ReflectionTestUtils.setField(controller, "questionSubmitService", questionSubmitService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void returnsOwnSubmission() throws Exception {
        User loginUser = user(10L);
        QuestionSubmit submission = submission(100L, 10L);
        QuestionSubmitVO vo = new QuestionSubmitVO();
        vo.setId(100L);
        vo.setUserId(10L);
        vo.setStatus(2);
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(loginUser);
        when(questionSubmitService.getById(100L)).thenReturn(submission);
        when(questionSubmitService.getQuestionSubmitVO(submission, loginUser)).thenReturn(vo);

        mockMvc.perform(get("/question_submit/get").param("id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.status").value(2));
    }

    @Test
    void rejectsOtherUsersSubmission() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(user(10L));
        when(questionSubmitService.getById(100L)).thenReturn(submission(100L, 20L));
        mockMvc.perform(get("/question_submit/get").param("id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101));
        verify(questionSubmitService, never()).getQuestionSubmitVO(any(), any());
    }

    @Test
    void returnsNotFoundAndRejectsInvalidId() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(user(10L));
        mockMvc.perform(get("/question_submit/get").param("id", "100"))
                .andExpect(jsonPath("$.code").value(40400));
        mockMvc.perform(get("/question_submit/get").param("id", "0"))
                .andExpect(jsonPath("$.code").value(40000));
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private QuestionSubmit submission(long id, long userId) {
        QuestionSubmit submission = new QuestionSubmit();
        submission.setId(id);
        submission.setUserId(userId);
        return submission;
    }
}
