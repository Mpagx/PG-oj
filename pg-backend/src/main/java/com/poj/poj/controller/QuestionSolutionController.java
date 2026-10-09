package com.poj.poj.controller;

import com.poj.poj.common.BaseResponse;
import com.poj.poj.common.DeleteRequest;
import com.poj.poj.common.ErrorCode;
import com.poj.poj.common.ResultUtils;
import com.poj.poj.exception.BusinessException;
import com.poj.poj.model.dto.questionsolution.QuestionSolutionSaveRequest;
import com.poj.poj.model.entity.User;
import com.poj.poj.model.vo.QuestionSolutionPageVO;
import com.poj.poj.service.QuestionSolutionService;
import com.poj.poj.service.UserService;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/question-solution")
public class QuestionSolutionController {
    @Resource private QuestionSolutionService solutionService;
    @Resource private UserService userService;

    @GetMapping("/page")
    public BaseResponse<QuestionSolutionPageVO> page(@RequestParam long questionId,
                                                      @RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long pageSize,
                                                      HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(solutionService.pageForUser(
                questionId, current, pageSize, user, userService.isAdmin(request)));
    }

    @PostMapping("/save")
    public BaseResponse<Long> save(@RequestBody QuestionSolutionSaveRequest body,
                                   HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(solutionService.saveMine(body, user, userService.isAdmin(request)));
    }

    @PostMapping("/reveal")
    public BaseResponse<Boolean> reveal(@RequestBody DeleteRequest body,
                                        HttpServletRequest request) {
        if (body == null || body.getId() == null || body.getId() <= 0) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        return ResultUtils.success(solutionService.reveal(
                body.getId(), user, userService.isAdmin(request)));
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> delete(@RequestBody DeleteRequest body, HttpServletRequest request) {
        if (body == null || body.getId() == null || body.getId() <= 0) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        return ResultUtils.success(solutionService.deleteSolution(
                body.getId(), user, userService.isAdmin(request)));
    }
}
