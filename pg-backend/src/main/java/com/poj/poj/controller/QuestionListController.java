package com.poj.poj.controller;

import com.poj.poj.common.BaseResponse;
import com.poj.poj.common.ErrorCode;
import com.poj.poj.common.ResultUtils;
import com.poj.poj.exception.BusinessException;
import com.poj.poj.model.dto.questionlist.QuestionListCreateRequest;
import com.poj.poj.model.dto.questionlist.QuestionListQuestionRequest;
import com.poj.poj.model.entity.User;
import com.poj.poj.model.vo.QuestionListVO;
import com.poj.poj.service.QuestionListService;
import com.poj.poj.service.UserService;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/question-list")
public class QuestionListController {
    @Resource private QuestionListService questionListService;
    @Resource private UserService userService;

    @PostMapping("/create")
    public BaseResponse<Long> create(@RequestBody QuestionListCreateRequest body, HttpServletRequest request) {
        if (body == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        return ResultUtils.success(questionListService.createQuestionList(body.getName(), user.getId()));
    }

    @GetMapping("/my")
    public BaseResponse<List<QuestionListVO>> mine(@RequestParam(required = false) Long questionId,
                                                    HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(questionListService.listMine(user.getId(), questionId));
    }

    @PostMapping("/question/add")
    public BaseResponse<Boolean> add(@RequestBody QuestionListQuestionRequest body, HttpServletRequest request) {
        validate(body);
        User user = userService.getLoginUser(request);
        return ResultUtils.success(questionListService.addQuestion(
                body.getQuestionListId(), body.getQuestionId(), user.getId()));
    }

    @PostMapping("/question/remove")
    public BaseResponse<Boolean> remove(@RequestBody QuestionListQuestionRequest body, HttpServletRequest request) {
        validate(body);
        User user = userService.getLoginUser(request);
        return ResultUtils.success(questionListService.removeQuestion(
                body.getQuestionListId(), body.getQuestionId(), user.getId()));
    }

    private void validate(QuestionListQuestionRequest body) {
        if (body == null || body.getQuestionListId() == null || body.getQuestionListId() <= 0
                || body.getQuestionId() == null || body.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
    }
}
